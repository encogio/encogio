(ns encogio.http
  (:require
   [encogio.core :as enc]
   [reitit.core :as r]
   [reitit.ring :as ring]))

(defn shorten-handler
  [req]
  (println :shorteninggg)
  {:status 200 :body ""})

(defn id-handler
  [req]
  (let [id (get-in req [:reitit.core/match :path-params :id])]
    (println (str :visited-id id)))
  {:status 200 :body ""})

(def router
  (ring/router
   [["" {:post shorten-handler}]
    ["/" {:post shorten-handler}]
    ["/:id" {:get id-handler}]]))

(def app
  (ring/ring-handler router (ring/create-default-handler)))

(comment
  (app {:request-method :post :uri ""})
  (app {:request-method :post :uri "/"})  
  (app {:request-method :get :uri "/abc+++"})
  (app {:request-method :post :uri "/abc"})  
  (app {:request-method :get :uri "/a"})  
  )




