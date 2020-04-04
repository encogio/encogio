(ns encogio.app
  (:require
   [encogio.http :as http]
   [ring.adapter.jetty :refer [run-jetty]]
   [taoensso.carmine :as car :refer [wcar]])
  (:gen-class))

(defn -main
  [& args]
  (let [port (Integer/valueOf (or (System/getenv "PORT") "8000"))
        redis-url (or (System/getenv "REDIS_URL") "127.0.0.1")
        redis-conn {:pool {} :spec {:url redis-url}}]
    (println (str "~> Starting encog.io on port " port ))
    (println (str "~> Pinging Redis...") (wcar redis-conn (car/ping)))
    (run-jetty http/app {:port port :join? false})))
