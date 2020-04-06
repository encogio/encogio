(ns encogio.config
  (:require [environ.core :refer [env]]))

(def redis-conn
  {:pool {} :spec {:url (env :redis-url "127.0.0.1")}})

(def port (Integer/valueOf (env :port "8000")))

