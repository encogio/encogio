(ns encogio.redis
  (:require
   [encogio.core :as enc]
   [taoensso.carmine :as car :refer [wcar]]))

(defn healthy?
  [conn]
  (try
    (= "PONG" (wcar conn (car/ping)))
    (catch Exception _ false)))

(defn- key-exists?
  [conn k]
  (= 1 (wcar conn (car/exists k))))

(defn- set-key!
  [conn k v]
  (if (key-exists? conn k)
    {:encogio.anomalies/category :encogio.anomalies/conflict}
    (let [[_ _ _ [set?]] (wcar conn
                           (car/watch k)
                           (car/multi)
                           (car/set k v)
                           (car/exec))]
      (if (= set? "OK")
        {:key k :value v}
        {:encogio.anomalies/category :encogio.anomalies/conflict}))))

;; keys

(def counter-key "encogio.counter")
(def id-prefix "encogio.id:")
(def rate-limit-prefix "encogio.ratelimit:")

(defn make-id-key
  [id]
  (str id-prefix id))

(defn make-rate-limit-key
  [id]
  (str rate-limit-prefix id))

;; urls

(defn- unique-id
  [conn]
  (enc/base-encode (wcar conn (car/incr counter-key))))

(defn store-url!
  ([conn url]
   (store-url! conn url (unique-id conn)))
  ([conn url id]
   (let [id-key (make-id-key id)
         result (set-key! conn id-key url)]
     (if (:encogio.anomalies/category result)
       (recur conn url (unique-id conn))
       {:url (:value result)
        :id id}))))
  
(defn alias-url!
  [conn url id]
  (let [id-key (make-id-key id)
        result (set-key! conn id-key url)]
    (if (:encogio.anomalies/category result)
      result
      {:url (:value result)
       :id id})))

(defn get-url!
  [conn id]
  (wcar conn
    (car/get (make-id-key id))))

(defn rate-limit
  [conn {:keys [limit
                limit-duration]} k]
  (let [key (make-rate-limit-key k)
        current (wcar conn (car/llen key))]
    (if (>= current limit)
      :limit
      (if (key-exists? conn key)
        (let [remaining (wcar conn
                          (car/rpushx key key))]
          {:remaining (- limit remaining)})
        (let [[_ _ _ [remaining _]] (wcar conn
                                      (car/multi)
                                      (car/rpush key key)
                                      (car/expire key limit-duration)
                                      (car/exec))]
          {:remaining (- limit remaining)})))))
