(ns encogio.http
  (:require
   [encogio.core :as enc]
   [encogio.url :as url]
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
      {:status 200 :body result})))

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

(defn make-router
  [conn]
  (ring/router
   [["" {:get home}]
    ["/" {:get home}]

    ;; todo: content negotiation, json/form
    ["/api"
     ["/shorten" {:post #(shorten conn %)}]
     ["/shorten/" {:post #(shorten conn %)}]]
    
    ["/:id" {:get #(redirect conn %)}]]))

(defn make-app
  [conn]
  (ring/ring-handler (make-router conn)
                     (ring/create-default-handler)))




