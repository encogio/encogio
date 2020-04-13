(ns encogio.admin
  (:require
   [encogio.i18n :as i18n]
   [encogio.html :as html]
   [encogio.time :as time]
   [encogio.http :as http]
   [encogio.redis :as redis]
   [encogio.config :as config]
   [encogio.url :as url]
   [buddy.hashers :as hashers]
   [reitit.ring.middleware.parameters :as params]
   [rum.core :as rum]
   [clojure.string :as string :refer [trim]]
   [reitit.middleware :as mid]
   [taoensso.carmine :as car :refer [wcar]]))

;; password

(def admin-password "encogio.admin.password:admin")
(def hasher-opts {:alg :pbkdf2+sha256})

(defn hash-password
  [pwd]
  (hashers/derive pwd hasher-opts))

(defn set-admin-password
  [conn pwd]
  (wcar conn
    (car/set admin-password (hash-password pwd))))

(defn check-admin-password
  [conn pwd]
  (let [derived (wcar conn
                  (car/get admin-password))]
    (hashers/check pwd derived hasher-opts)))

;; ui

(rum/defc clients-table
  [tr clients]
  [:table.table.is-fullwidth.is-hoverable.is-bordered.is-striped
   [:thead
    [:tr.is-selected.is-info
     [:th (tr [:admin/ip])]
     [:th (tr [:admin/requests])]
     [:th (tr [:admin/ttl])]]]
   [:tbody
    (for [[ip {:keys [hits ttl]}] (sort-by first clients)]
      [:tr
       [:th ip]
       [:th hits]
       [:th (time/seconds->duration ttl tr)]])]])

(rum/defc login-attempts-table
  [tr clients]
  [:table.table.is-fullwidth.is-hoverable.is-bordered.is-striped
   [:thead
    [:tr.is-selected.is-info
     [:th (tr [:admin/ip])]
     [:th (tr [:admin/login-attempts])]
     [:th (tr [:admin/ttl])]]]
   [:tbody
    (for [[ip {:keys [hits ttl]}] (sort-by first clients)]
      [:tr
       [:th ip]
       [:th hits]
       [:th (time/seconds->duration ttl tr)]])]])

(rum/defc panel
  [tr {:keys [urls clients healthy? rate-limit site]}]
  [:section.section
   [:nav.level
    [:.level-item.has-text-centered
     [:div
      [:p.heading (tr [:admin/site])]
      [:a.title
       {:href (url/site-root site)}
       (:host site)]]]
    [:.level-item.has-text-centered
     [:div
      [:p.heading (tr [:admin/db])]
      [:p.title
       (if healthy?
         [:span.icon.has-text-success
          [:i.fas.fa-check]]
         [:span.icon.has-text-danger
          [:i.fas.fa-ban]])]]]
    [:.level-item.has-text-centered
     [:div
      [:p.heading (tr [:admin/urls])]
      [:p.title urls]]]
    [:.level-item.has-text-centered
     [:div
      [:p.heading (tr [:admin/rate-limit])]
      [:p.title
       (str (:limit rate-limit) " / " (time/seconds->unit (:limit-duration rate-limit) tr))]]]
    [:.level-item.has-text-centered
     [:div
      [:p.heading (tr [:admin/clients])]
      [:p.title clients]]]]])

(rum/defc admin-panel-html
  [tr stats config api-clients login-clients]
  (html/page {:title (tr [:admin/title])}
             [:body
              (panel tr (merge stats config))
              [:section.section
               [:.columns
                [:.column.is-half.has-text-centered
                 [:h2.title (tr [:admin/api-clients])
                  (clients-table tr api-clients)]]
                [:.column.is-half.has-text-centered
                 [:h2.title (tr [:admin/login-attempts])
                  (login-attempts-table tr login-clients)]]]]]))

(rum/defc password-form
  [tr mode message]
  [:form.box
   {:action "/admin/panel" :method "post"}
   [:.field
    [:label.label (tr [:admin/password])]
    [(case mode
       :danger
       :input.input.is-danger

       :warning
       :input.input.is-warning

       :input.input)
     {:type "password"
      :id "password"
      :name "password"
      :required true}]]
   (when message
     [(case mode
        :danger
        :p.help.is-danger

        :warning
        :p.help.is-warning

        :p.help)
      message])
   [:field
    [:input.button.button.is-info.is-centered 
     {:type "submit"}]]])

(rum/defc admin-login-form
  [tr mode message]
  (html/page {:title (tr [:admin/login])}
             [:body
              [(case mode
                 :danger
                 :section.hero.is-danger.is-fullheight

                 :warning
                 :section.hero.is-warning.is-fullheight

                 :section.hero.is-primary.is-fullheight)
               [:.hero-body
                [:.container
                 [:.columns.is-centered
                  [:.column
                   (password-form tr mode message)]]]]]]))

(defn admin-panel-handler
  [tr conn]
  (let [api-clients (redis/get-rate-limits conn)
        login-attempts (redis/get-rate-limits conn
                                              "encogio.admin.login-attempts:*"
                                              "encogio.admin.login-attempts:")
        stats (redis/stats conn)
        cfg {:site config/site
             :rate-limit config/rate-limit}]
    {:status 200
     :body (rum/render-static-markup (admin-panel-html tr stats cfg api-clients login-attempts))
     :headers {"Content-Type" "text/html"}}))

(defn admin-login-handler
  ([tr]
   (admin-login-handler tr :info nil))
  ([tr mode message]
   {:status 401
    :body (rum/render-static-markup (admin-login-form tr mode message))
    :headers {"Content-Type" "text/html"}}))

(def login-attempts-settings {:limit 3
                              :limit-duration 3600
                              :prefix "encogio.admin.login-attempts:"})

(rum/defc rate-limit
  [retry-after tr]
  (html/page {:title (tr [:admin/rate-limit])}
             [:body
              [:section.hero.is-danger.is-fullheight.has-text-centered
               [:.hero-body
                [:.container.has-text-centered
                 [:h1.title (tr [:admin/retry-after] [(time/seconds->duration retry-after tr)])]]]]]))

(defn rate-limit-handler
  [retry-after tr]
  {:status 429
   :body (rum/render-static-markup (rate-limit retry-after tr))
   :headers {"Retry-After" (str retry-after)
             "Content-Type" "text/html"}})

(defn login-attempts-middleware
  [conn]
  (mid/map->Middleware
   {:name ::login-attempts
    :description "Middleware that limits login attempts by IP"
    :wrap (fn [handler]
            (fn [request]
              (if (= :post
                     (:request-method request))
                ;; login attempt
                (if-let [ip (http/request->ip request)]
                  (let [limited? (redis/limited? conn login-attempts-settings ip)
                        tr (i18n/request->tr request)]
                    (if limited?
                      (rate-limit-handler (redis/ttl conn login-attempts-settings ip) tr)
                      (let [response (handler request)]
                        (if (= 200 (:status response))
                          response ;; todo: successful login, reset limit?
                          (let [[cmd res] (redis/rate-limit conn login-attempts-settings ip)]
                            (if (redis/limited? conn login-attempts-settings ip)
                              (rate-limit-handler (redis/ttl conn login-attempts-settings ip) tr)
                              (admin-login-handler tr :danger (tr [:admin/attempts] [res]))))))))
                  ;; no ip available (dev)
                  (handler request))
                ;; not login attempt
                (handler request))))}))

(defn try-login
  [conn req]
  (if-let [pwd (get-in req [:form-params "password"])]
    (if (check-admin-password conn pwd)
      (admin-panel-handler (i18n/request->tr req) conn)
      (admin-login-handler (i18n/request->tr req) :warning "Wrong password"))
    (admin-login-handler (i18n/request->tr req) :danger "Password required")))

(defn route
  [conn]
  ["/admin"
   ["/panel"
    {:no-doc true
    :get
    (fn [req]
      (admin-login-handler (i18n/request->tr req)))
    :post
    (fn [req]
      (try-login conn req))
     :middleware [i18n/middleware
                  (login-attempts-middleware conn)
                  params/parameters-middleware]}]])
