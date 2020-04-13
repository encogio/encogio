(ns encogio.server
  (:require
   [encogio.api :as api]
   [encogio.http :as http]
   [encogio.config :as config]
   [encogio.redis :as redis]
   [encogio.admin :as admin]
   [reitit.ring :as ring]
   [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))

(def router
  (ring/router
   [["" {:get http/home :no-doc true}]
    ["/" {:get http/home :no-doc true}]
    (admin/route config/redis-conn)
    (api/routes config/redis-conn)]))

(def app
  (ring/ring-handler router
                     (ring/routes
                      (ring/create-resource-handler {:root "public" :path "/"})
                      (ring/create-default-handler))))

(defn -main
  [& args]
  (when-not (redis/healthy? config/redis-conn)
    (throw (Exception.
            (str "Unable to connect to redis with settings: " (pr-str config/redis-conn)))))
  (run-jetty app {:port config/port :join? false}))

(comment
  (def server (-main))
  (.stop server)
  )
