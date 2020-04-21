(ns encogio.server
  (:require
   [encogio.config :as config]
   [encogio.ring :as ring]
   [encogio.redis :as redis]
   [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))

(defn -main
  [& _]
  (when-not (redis/healthy? config/redis-conn)
    (throw (Exception.
            (str "Unable to connect to redis with settings: " (pr-str config/redis-conn)))))
  (run-jetty ring/app {:port config/port :join? false}))

(comment
  (def server (-main))
  (.stop server)
  )
