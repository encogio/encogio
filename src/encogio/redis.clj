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
(def url-prefix "encogio.url:")
(def rate-limit-prefix "encogio.ratelimit:")

(defn make-url-key
  ([id]
   (str url-prefix "default:" id))
  ([domain id]
   (str url-prefix domain ":" id)))

(defn make-rate-limit-key
  ([id]
   (str rate-limit-prefix id))
  ([prefix id]
   (str prefix id)))

(defn- unique-id
  [conn]
  (enc/base-encode (wcar conn (car/incr counter-key))))

(defn store-url!
  ([conn url]
   (store-url! conn url (unique-id conn)))
  ([conn url id]
   (let [id-key (make-url-key id)
         result (set-new-key! conn id-key url)]
     (if (an/conflict? result)
       (recur conn url (unique-id conn))
       {:url (:value result)
        :id id}))))
  
(defn alias-url!
  [conn url id]
  (let [id-key (make-url-key id)
        result (set-new-key! conn id-key url)]
    (if (an/conflict? result)
      result
      {:url (:value result)
       :id id})))

(defn get-url!
  ([conn id]
   (wcar conn
     (car/get (make-url-key id))))
  ([conn domain id]
   (wcar conn
     (car/get (make-url-key domain id)))))

(defn rate-limit
  [conn {:keys [limit
                limit-duration
                prefix]
         :or {prefix rate-limit-prefix}} k]
  (let [key (make-rate-limit-key prefix k)
        [result response]
        (wcar conn
          (car/eval* rate-limit-lua 3 key limit limit-duration))]
    (if (= result "OK")
      [:ok response]
      [:limit response])))

(defn limited?
  [conn {:keys [limit
                prefix]
         :or {prefix rate-limit-prefix}} k]
  (let [key (make-rate-limit-key prefix k)
        current (wcar conn (car/llen key))]
    (= current limit)))

(defn ttl
  [conn {:keys [prefix]
         :or {prefix rate-limit-prefix}} k]
  (let [key (make-rate-limit-key prefix k)]
    (wcar conn (car/ttl key))))

(defn scan-keys
  [conn pattern cursor]
  (wcar conn
    (car/scan cursor
              :match pattern
              :count 1000)))

(defn scan-all
  "
    - (fn rf      [acc scan-result]) -> next accumulator
    - (fn scan-fn [cursor]) -> next scan result"
  [rf init scan-fn]
  (loop [cursor "0"
         acc init]
    (let [[next-cursor in] (scan-fn cursor)]
      (if (= next-cursor "0")
        ;; last
        (if (empty? in)
          acc
          (rf acc in))
        ;; continue
        (recur next-cursor (rf acc in))))))

(defn scan-match
  [rf init pattern conn]
  (scan-all rf init #(scan-keys conn pattern %)))

(defn count-urls
  [conn]
  (scan-match
   (fn [acc in]
     (+ acc (count in)))
   0
   "encogio.url:default:*"
   conn))

(defn count-clients
  [conn]
  (scan-match
   (fn [acc in]
     (+ acc (count in)))
   0
   "encogio.ratelimit:*"
   conn))

(defn remove-prefix
  [s prf]
  (subs s (count prf)))

(defn get-rate-limits
  ([conn]
   (get-rate-limits conn "encogio.ratelimit:*" rate-limit-prefix))
  ([conn pattern prefix]
   (scan-match
    (fn [acc client-keys]
      (if (= 1 (count client-keys))
        (let [k (first client-keys)
              [hits ttl] (wcar conn
                           (car/llen k)
                           (car/ttl k))
              client (remove-prefix k prefix)]
          (conj acc [client {:hits hits :ttl ttl}]))
        (let [hits (wcar conn
                     (mapv car/llen client-keys))
              ttl (wcar conn
                    (mapv car/ttl client-keys))
              clients (map #(remove-prefix % prefix) client-keys)
              stats (map (fn [h t] {:hits h :ttl t}) hits ttl)]
          (into acc (zipmap clients stats)))))
    #{}
    pattern
    conn)))

(defn stats
  [conn]
  {:clients (count-clients conn)
   :urls (count-urls conn)
   :healthy? (healthy? conn)})
