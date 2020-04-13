(ns encogio.admin
  (:require
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
  [clients]
  [:table.table.is-fullwidth.is-hoverable.is-bordered.is-striped
   [:thead
    [:tr.is-selected.is-info
     [:th "IP address"]
     [:th "Hits"]
     [:th "TTL"]]]
   [:tbody
    (for [[ip {:keys [hits ttl]}] (sort-by first clients)]
      [:tr
       [:th ip]
       [:th hits]
       [:th (time/seconds->duration ttl)]])]])

(rum/defc panel
  [{:keys [urls clients healthy? rate-limit site]}]
  [:section.section
   [:nav.level
    [:.level-item.has-text-centered
     [:div
      [:p.heading "Site"]
      [:a.title
       {:href (url/site-root site)}
       (:host site)]]]
    [:.level-item.has-text-centered
     [:div
      [:p.heading "DB"]
      [:p.title
       (if healthy?
         [:span.icon.has-text-success
          [:i.fas.fa-check]]
         [:span.icon.has-text-danger
          [:i.fas.fa-ban]])]]]
    [:.level-item.has-text-centered
     [:div
      [:p.heading "URLs"]
      [:p.title urls]]]
    [:.level-item.has-text-centered
     [:div
      [:p.heading "Rate limit"]
      [:p.title
       (str (:limit rate-limit) " / " (time/seconds->unit (:limit-duration rate-limit)))
       ]]]
    [:.level-item.has-text-centered
     [:div
      [:p.heading "Clients"]
      [:p.title clients]]]]])

(rum/defc admin-panel-html
  [stats config clients]
  [:html
   [:head
    [:title "Encog.io admin panel"]
    [:link {:rel "stylesheet" :href "/css/font-awesome.css"}]
    [:link {:rel "stylesheet" :href "/css/main.css"}]]
   [:body
    (panel (merge stats config))
    [:section.section
     [:.columns
      [:.column.is-half.is-offset-one-quarter
       (clients-table clients)]]]]])

(rum/defc password-form
  [mode message]
  [:form.box
   {:action "/admin/panel" :method "post"}
   [:.field
    [:label.label "Password"]
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
   [:field
    [:input.button.button.is-info.is-centered 
     {:type "submit"}]]])

(rum/defc admin-login-form
  [mode message]
  [:html
   [:head
    [:title "Encog.io admin login"]
    [:link {:rel "stylesheet" :href "/css/font-awesome.css"}]
    [:link {:rel "stylesheet" :href "/css/main.css"}]]
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
         (password-form mode message)]]]]]]])

(defn admin-panel-handler
  [conn]
  (let [clients (redis/get-clients conn)
        stats (redis/stats conn)
        cfg {:site config/site
             :rate-limit config/rate-limit}]
    {:status 200
     :body (rum/render-static-markup (admin-panel-html stats cfg clients))
     :headers {"Content-Type" "text/html"}}))

(defn admin-login-handler
  ([req]
   (admin-login-handler req :info nil))
  ([req mode message]
   {:status 200
    :body (rum/render-static-markup (admin-login-form mode message))
    :headers {"Content-Type" "text/html"}}))

(defn try-login
  [conn req]
  (if-let [pwd (get-in req [:form-params "password"])]
    (if (check-admin-password conn pwd)
      (admin-panel-handler conn)
      (admin-login-handler req :warning "Wrong password"))
    (admin-login-handler req :danger "Password required")))


(def login-attempts-settings {:limit 3
                              :limit-duration 3600
                              :prefix "encogio.admin.login-attempts:"})

#_(defn rate-limit-middleware
  [conn]
  (mid/map->Middleware
   {:name ::rate-limit
    :description "Middleware that rate limits by IP"
    :wrap (fn [handler]
            (fn [request]
              ;; todo: rate limit only on POST, use login handlers instead of default
              (if-let [ip (http/request->ip request)]
                (let [[limit ttl] (redis/rate-limit conn settings ip)]
                  (if (= limit :limit)
                    {:status 429
                     :headers {"Retry-After" (str ttl)}}
                    (handler request)))
                (handler request))))}))

(defn route
  [conn]
  ["/admin"
   ["/panel"
    {:no-doc true
    :get
    (fn [req]
      (admin-login-handler req))
    :post
    (fn [req]
      (try-login conn req))
    :middleware [params/parameters-middleware]}]])
    
