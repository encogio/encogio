(ns encogio.http
  (:require
   [encogio.core :as enc]
   [encogio.url :as url]
   [encogio.config :as config]
   [encogio.redis :as redis]
   [ring.util.request :refer [body-string]]   
   [ring.util.response :refer [resource-response]]
   [reitit.core :as r]
   [reitit.ring :as ring]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [muuntaja.core :as m]))

;; todo: rate limit

(defn shorten-handler
  [conn url]
  (let [result (redis/store-url! conn url)]
    (if (:encogio.anomalies/category result)
      {:status 500}
      {:status 200
       :body {:url url
              :short-url (url/urlize (:id result))}})))

(defn shorten
  [conn req]
  (let [body (:body-params req)
        raw-url (:url body)]
    (if-let [u (url/validate raw-url)]
      (shorten-handler conn u)
      {:status 400 :body "Invalid URL"})))
    
(defn redirect-handler
  [conn id]
  (if-let [url (redis/get-url! conn id)]
    (ring.util.response/redirect url)
    {:status 404}))

(defn redirect
  [conn {:keys [path-params]}]
  (let [id (:id path-params)]
    (if (enc/valid-word? id)
      (redirect-handler conn id)
      {:status 404})))

(defn home
  [req]
  (resource-response "index.html" {:root "public"}))
(def content-negotiation
  (m/create
   (m/select-formats
    m/default-options
    ["application/json"
     "application/edn"])))

(def content-negotiation-middleware
  [muuntaja/format-negotiate-middleware
   muuntaja/format-response-middleware
   muuntaja/format-request-middleware])

(def router
  (ring/router
   [["" {:get home}]
    ["/" {:get home}]

    ["/api" {:muuntaja content-negotiation
             :middleware content-negotiation-middleware}
     ["/shorten" {:post #(shorten config/redis-conn %)}]
     ["/shorten/" {:post #(shorten config/redis-conn %)}]]
    
    ["/:id" {:get #(redirect config/redis-conn %)}]]))

(def app
  (ring/ring-handler router
                     (ring/routes
                       (ring/create-resource-handler {:root "public" :path "/"})
                       (ring/create-default-handler))))
