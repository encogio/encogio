(ns encogio.ring
  (:require
   [encogio.api :as api]
   [encogio.http :as http]
   [encogio.html :as html]
   [encogio.config :as config]
   [encogio.redis :as redis]
   [encogio.admin :as admin]
   [encogio.i18n :as i18n]
   [reitit.ring :as ring]))

(defn home
  [req]
  {:status 200
   :body (html/render-home config/site (i18n/request->tr req))
   :headers {"Content-Type" "text/html"}})

(def router
  (ring/router
   [["" {:get home
         :no-doc true
         :middleware [i18n/middleware]}]
    ["/" {:get home
          :no-doc true
          :middleware [i18n/middleware]}]
    (admin/route config/redis-conn)
    (api/routes config/redis-conn)]))

(def app
  (ring/ring-handler router
                     (ring/routes
                      (ring/create-resource-handler {:root "public" :path "/"})
                      (ring/create-default-handler))))
