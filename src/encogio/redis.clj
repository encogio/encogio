(ns encogio.redis
  (:require
   [clojure.java.io :as io]
   [encogio.core :as enc]
   [encogio.anomalies :as an]
   [taoensso.carmine :as car :refer [wcar]]))

(def atomic-set-lua (slurp (io/resource "lua/atomic-set.lua")))
(def rate-limit-lua (slurp (io/resource "lua/rate-limit.lua")))

(defn healthy?
  [conn]
  (try
    (= "PONG" (wcar conn (car/ping)))
    (catch Exception _ false)))

(defn- set-new-key!
  [conn k v]
  (let [set? (wcar conn (car/eval* atomic-set-lua 2 k v))]
    (if (= set? "OK")
      {:key k :value v}
      (an/conflict "Can't set duplicate keys"))))

(def counter-key "encogio.counter")
(def id-prefix "encogio.id:")
(def rate-limit-prefix "encogio.ratelimit:")

(defn make-id-key
  [id]
  (str id-prefix id))

(defn make-rate-limit-key
  [id]
  (str rate-limit-prefix id))

(defn- unique-id
  [conn]
  (enc/base-encode (wcar conn (car/incr counter-key))))

(defn store-url!
  ([conn url]
   (store-url! conn url (unique-id conn)))
  ([conn url id]
   (let [id-key (make-id-key id)
         result (set-new-key! conn id-key url)]
     (if (an/conflict? result)
       (recur conn url (unique-id conn))
       {:url (:value result)
        :id id}))))
  
(defn alias-url!
  [conn url id]
  (let [id-key (make-id-key id)
        result (set-new-key! conn id-key url)]
    (if (an/conflict? result)
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
        [result response]
        (wcar conn
          (car/eval* rate-limit-lua 3 key limit limit-duration))]
    (if (= result "OK")
      [:ok response]
      [:limit response])))
