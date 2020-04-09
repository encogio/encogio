(ns encogio.config
  (:require [environ.core :refer [env]]))

(def site
  {:scheme "http"
   :host (env :site-host "encog.io")})

(def rate-limit
  {:limit 1000
   :limit-duration 3600})

(def redis-conn
  {:pool {}
   :spec
   (if-let [uri (env :redis-url)]
     {:uri uri}
     {:url "127.0.0.1"})})


(def port (Integer/valueOf (env :port "8000")))
