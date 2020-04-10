(ns encogio.http
  (:require
   [encogio.core :as enc]
   [encogio.url :as url]
   [encogio.config :as config]
   [encogio.redis :as redis]
   [encogio.auth :as auth]
   [clojure.string :as s]
   [ring.util.response :as resp :refer [resource-response]]
   [reitit.ring :as ring]
   [reitit.middleware :as mid]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [muuntaja.core :as m]))

(defn shorten-handler
  [conn url]
  (let [result (redis/store-url! conn url)]
    (if (:encogio.anomalies/category result)
      {:status 500}
      {:status 200
       :body {:url url
              :alias (:id result)
              :short-url (url/urlize config/site (:id result))}})))

(defn alias-handler
  [conn url alias]
  (let [result (redis/alias-url! conn url alias)]
    (if (:encogio.anomalies/category result)
      {:status 409}
      {:status 200
       :body {:url url
              :alias alias
              :short-url (url/urlize config/site (:id result))}})))


(defn shorten
  [conn req]
  (let [{:keys [url alias]} (:body-params req)]
    (if-let [valid-url (url/validate url)]
      (let [host (.getHost valid-url)
            url-str (.toString valid-url)]
        (if (= host (:host config/site))
          {:status 403}
          (if alias
            (if (enc/valid-word? alias)
              (alias-handler conn url-str alias)
              {:status 400
               :body {:code "invalid-alias"
                      :cause "Invalid alias"}})
            (shorten-handler conn url-str))))
      {:status 400
       :body {:code "invalid-url"
              :cause "Invalid URL"}})))
    
(defn redirect-handler
  [conn id]
  (if-let [url (redis/get-url! conn id)]
    (resp/redirect url :permanent-redirect)
    {:status 404}))

(defn redirect
  [conn {:keys [path-params]}]
  (let [id (:id path-params)]
    (if (enc/valid-word? id)
      (redirect-handler conn id)
      {:status 404})))

(defn home
  [_]
  (resource-response "index.html" {:root "public"}))

(def content-negotiation
  (m/create
   (m/select-formats
    m/default-options
    ["application/json"])))

(def content-negotiation-middleware
  [muuntaja/format-negotiate-middleware
   muuntaja/format-response-middleware
   muuntaja/format-request-middleware])

(defn rate-limit-middleware
  [conn settings]
  (mid/map->Middleware
   {:name ::rate-limit
    :description "Middleware that rate limits by IP"
    :wrap (fn [handler]
            (fn [request]
              (if-let [forwarded-for (get-in request [:headers "x-forwarded-for"])]
                (let [remote-addr (s/trim (re-find #"[^,]*" forwarded-for))
                      limit (redis/rate-limit conn settings remote-addr)]
                  (if (= limit :limit)
                    {:status 429}
                    (handler request)))
                (handler request))))}))

(def api-middleware
  [muuntaja/format-negotiate-middleware
   muuntaja/format-response-middleware
   muuntaja/format-request-middleware
   (rate-limit-middleware config/redis-conn config/rate-limit)])

(def router
  (ring/router
   [["" {:get home}]
    ["/" {:get home}]

    ["/api" {:muuntaja content-negotiation
             :middleware api-middleware}
     ["/shorten" {:post {:middleware auth/auth-middleware
                         :handler #(shorten config/redis-conn %)}}]]
    
    ["/:id" {:get #(redirect config/redis-conn %)}]]))

(def app
  (ring/ring-handler router
                     (ring/routes
                      (ring/create-resource-handler {:root "public"
                                                     :path "/"})
                      (ring/create-default-handler))))
