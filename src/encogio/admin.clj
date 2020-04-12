(ns encogio.admin
  (:require
   [buddy.hashers :as hashers]
   [reitit.ring.middleware.parameters :as params]
   [rum.core :as rum]
   [clojure.string :as string :refer [trim]]
   [encogio.redis :as redis]
   [encogio.config :as config]
   [encogio.url :as url]
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

(def minute 60)
(def hour (* 60 minute))
(def day (* 24 hour))
(def week (* 7 day))
 
(defn seconds->duration
  [seconds]
  (let [weeks   ((juxt quot rem) seconds week)
        wk      (first weeks)
        days    ((juxt quot rem) (last weeks) day)
        d       (first days)
        hours   ((juxt quot rem) (last days) hour)
        hr      (first hours)
        min     (quot (last hours) minute)
        sec     (rem (last hours) minute)]
    (string/join ", "
                 (filter #(not (string/blank? %))
                         (conj []
                               (when (> wk 0) (str wk " week"))
                               (when (> d 0) (str d " day"))
                               (when (> hr 0) (str hr " hour"))
                               (when (> min 0) (str min " min")))))))

(defn seconds->unit
  [seconds]
  (let [weeks   ((juxt quot rem) seconds week)
        wk      (first weeks)
        days    ((juxt quot rem) (last weeks) day)
        d       (first days)
        hours   ((juxt quot rem) (last days) hour)
        hr      (first hours)
        min     (quot (last hours) minute)
        sec     (rem (last hours) minute)]
    (string/join ", "
                 (filter #(not (string/blank? %))
                         (conj []
                               (when (= wk 1) "week")
                               (when (= d 1) "day")
                               (when (= hr 1) "hour")
                               (when (= min 1) "min")
                               (when (= sec 1) "sec"))))))

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
       [:th (seconds->duration ttl)]])]])

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
       (str (:limit rate-limit) " / " (seconds->unit (:limit-duration rate-limit)))
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
    
