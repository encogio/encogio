(ns encogio.redis.rate-limit
  (:require
   [clojure.java.io :as io]
   [encogio.redis :as redis]
   [taoensso.carmine :as car :refer [wcar]]))

(def rate-limit-lua (slurp (io/resource "lua/rate-limit.lua")))

(def rate-limit-prefix "encogio.rate-limit:")

(defn make-rate-limit-key
  ([id]
   (str rate-limit-prefix id))
  ([prefix id]
   (str prefix id)))

(defn rate-limit
  [conn {:keys [limit
                limit-duration
                prefix]
         :or {prefix rate-limit-prefix}} k]
  (let [key (make-rate-limit-key prefix k)
        [result response]
        (wcar conn
          (car/eval* rate-limit-lua 1 key limit limit-duration))]
    (if (= result "OK")
      [:ok response]
      [:limit response])))

(defn str->int
  [s]
  (when s
    (Integer/valueOf s)))

(defn limited?
  [conn {:keys [limit
                prefix]
         :or {prefix rate-limit-prefix}} k]
  (let [key (make-rate-limit-key prefix k)]
    (when-let [current (str->int
                        (wcar conn (car/get key))) ]
      (= current limit))))

(defn ttl
  [conn {:keys [prefix]
         :or {prefix rate-limit-prefix}} k]
  (let [key (make-rate-limit-key prefix k)]
    (wcar conn (car/ttl key))))

(defn count-rate-limits
  [conn]
  (redis/scan-match
   (fn [acc in]
     (+ acc (count in)))
   0
   (str rate-limit-prefix "*")
   conn))

(defn remove-prefix
  [s prf]
  (subs s (min (count prf) (count s))))

(defn get-rate-limits
  ([conn]
   (get-rate-limits conn rate-limit-prefix))
  ([conn prefix]
   (redis/scan-match
    (fn [acc client-keys]
      (if (= 1 (count client-keys))
        (let [k (first client-keys)
              hits (str->int (wcar conn
                               (car/get k)))
              ttl (wcar conn
                    (car/ttl k))
              client (remove-prefix k prefix)]
          (conj acc [client {:hits hits :ttl ttl}]))
        (let [hits (mapv str->int
                         (wcar conn
                           (apply car/mget client-keys)))
              ttl (wcar conn
                    (mapv car/ttl client-keys))
              clients (map #(remove-prefix % prefix) client-keys)
              stats (map (fn [h t] {:hits h :ttl t}) hits ttl)]
          (into acc (zipmap clients stats)))))
    #{}
    (str prefix "*")
    conn)))
