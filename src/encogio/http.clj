(ns encogio.http
  (:require
   [encogio.core :as enc]
   [encogio.url :as url]
   [encogio.config :as config]
   [encogio.redis :as redis]
   [ring.util.request :refer [body-string]]   
   [ring.util.response :refer [resource-response]]
   [reitit.core :as r]
   [reitit.ring :as ring]))

;; todo: rate limit

(defn shorten-handler
  [conn url]
  (let [result (redis/store-url! conn url)]
    (if (:encogio.anomalies/category result)
      {:status 500}
      {:status 200
       :body (url/urlize (:id result))
       :headers {"Content-Type" "text/plain"}})))

(defn shorten
  [conn req]
  (if-let [url (url/validate (body-string req))]
    (shorten-handler conn url)
    {:status 400}))
    
(defn redirect-handler
  [conn id]
  (if-let [url (redis/get-url! conn id)]
    (ring.util.response/redirect url)
    {:status 404}))

(defn redirect
  [conn req]
  (let [id (get-in req [:reitit.core/match :path-params :id])]
    (if (enc/valid-word? id)
      (redirect-handler conn id)
      {:status 404})))

(defn home
  [req]
  (resource-response "index.html" {:root "public"}))

(def router
  (ring/router
   [["" {:get home}]
    ["/" {:get home}]

    ;; todo: content negotiation, json/form
    ["/api"
     ["/shorten" {:post #(shorten config/redis-conn %)}]
     ["/shorten/" {:post #(shorten config/redis-conn %)}]]
    
    ["/:id" {:get #(redirect config/redis-conn %)}]]))

(def app
  (ring/ring-handler router
                     (ring/routes
                       (ring/create-resource-handler {:root "public" :path "/"})
                       (ring/create-default-handler))))
