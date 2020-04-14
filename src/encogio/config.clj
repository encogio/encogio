(ns encogio.config
  (:require [environ.core :refer [env]]
            [encogio.time :as time]))

(def secret-key
  (env :secret-key "4eUE0og3wbxcTfWJYdKhpPY41mAakZ3oC63ngT4u"))

(def site
  {:scheme "http"
   :host (env :site-host "encog.io")})

(def rate-limit
  {:limit 100
   :limit-duration time/day})

(def redis-conn
  {:pool {}
   :spec
   (if-let [uri (env :redis-url)]
     {:uri uri}
     {:url "127.0.0.1"})})


(def port (Integer/valueOf (env :port "8000")))
