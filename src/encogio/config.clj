(ns encogio.config
  (:require [environ.core :refer [env]]
            [encogio.time :as time]))

(def secret-key
  (env :secret-key "4eUE0og3wbxcTfWJYdKhpPY41mAakZ3oC63ngT4u"))

(def site
  {:scheme (env :site-scheme "http")
   :host (env :site-host "localhost")})

(def rate-limit
  {:limit 100
   :limit-duration time/day})

(def redis-conn
  {:pool {}
   :spec
   (if-let [uri (env :redis-url)]
     {:uri uri}
     {})})


(def port (Integer/valueOf (env :port "8000")))
