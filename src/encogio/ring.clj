(ns encogio.ring
  (:require
   [encogio.api :as api]
   [encogio.html :as html]
   [encogio.config :as config]
   [encogio.admin :as admin]
   [encogio.i18n :as i18n]
   [reitit.ring :as ring]))

(defn home
  [req]
  {:status 200
   :body (html/render-home config/site (i18n/request->tr req))
   :headers {"Content-Type" "text/html"}})

(defn conn->router
  [conn]
  (ring/router
   [["" {:get home
         :no-doc true
         :middleware [i18n/middleware]}]
    ["/" {:get home
          :no-doc true
          :middleware [i18n/middleware]}]
    (admin/route conn)
    (api/routes conn)]))

(defn conn->app
  [conn]
  (ring/ring-handler (conn->router conn)
                     (ring/routes
                      (ring/create-resource-handler {:root "public" :path "/"})
                      (ring/create-default-handler))))

(def app
  (conn->app encogio.config/redis-conn))


