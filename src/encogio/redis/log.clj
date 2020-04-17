(ns encogio.redis.log
  (:require
   [taoensso.carmine :as car :refer [wcar]]
   [clojure.string :as s])
  (:import [java.time Instant]))

(def links-stream "encogio.links.stream")

(defn stream-id->instant
  "Convert a Redis Stream ID into a `java.time.Instant`."
  [^String id]
  (let [millis (Long/valueOf (first (s/split id #"-")))]
    (Instant/ofEpochMilli millis)))

(defn instant->stream-id
  "Convert a `java.time.Instant` into a Redis Stream ID."
  ([^Instant inst]
   (.toEpochMilli inst)))

(defn add-link!
  [conn {:keys [id url]}]
  (wcar conn
    (car/xadd links-stream
              "*"
              "id" id
              "url" url)))

(defn- redis-hash->link
  [[id [_ link-id _ url]]]
  {:inst (stream-id->instant id)
   :url url
   :id link-id})

(defn get-latest-links!
  ([conn]
   (get-latest-links! conn 10))
  ([conn n]
   (let [links (wcar conn
                 (car/xrevrange links-stream
                                "+"
                                "-"
                                :count n))]
     (map redis-hash->link links))))

(defn get-links-between!
  [conn start end]
  (let [links (wcar conn
                 (car/xrange links-stream
                             (instant->stream-id start)
                             (instant->stream-id end)))]
    (mapv redis-hash->link links)))

(defn get-all-links!
  [conn]
  (let [links (wcar conn
                (car/xrange links-stream "-" "+"))]
    (map redis-hash->link links)))

(comment
 (def i (Instant/now))

 (add-link! {} {:id "b" :url "http://foo.bar"})

 (get-latest-links! {} 2)

 (get-links-between! {}
                     i
                     (.plusSeconds i ( * 2 3600))))
