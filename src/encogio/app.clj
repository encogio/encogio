(ns encogio.app
  (:require
   [encogio.http :as http]
   [encogio.config :as config]
   [encogio.redis :as redis]
   [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))

(defn -main
  [& args]
  (when-not (redis/healthy? config/redis-conn)
    (throw (Exception.
            (str "Unable to connect to redis with settings: " (pr-str config/redis-conn)))))
  (run-jetty http/app {:port config/port :join? false}))

(comment
  (def server (-main))
  (.stop server)
  )
