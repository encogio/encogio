(ns encogio.redis.stats
  (:require
   [clojure.string :as s]
   [taoensso.carmine :as car :refer [wcar]]))



(defn info
  [conn]
  (into {}
        (for [s (s/split (wcar conn
                           (car/info "all"))
                         #"\r\n")
              :when (and
                     (not (s/blank? s))
                     (not (.startsWith s "#")))
              :let [[raw-id value] (s/split s #":")
                    id (s/replace raw-id \_ \-)]]
          [(keyword id) value])))

(comment
  (keys (info {}))
  (:used-memory-human (info {}))
  (:used-memory-scripts-human (info {}))
  (:total-commands-processed (info {}))
  (:keyspace-hits (info {}))
  (:keyspace-misses (info {})))
