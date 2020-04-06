(ns encogio.app
  (:require
   [encogio.http :as http]
   [encogio.config :as config]
   [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))

(defn -main
  [& args]
  (run-jetty http/app {:port config/port :join? false}))

(comment
  (def server (-main))
  (.stop server)
  )
