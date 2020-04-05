(ns encogio.redis
  (:require
   [encogio.core :as enc]
   [taoensso.carmine :as car :refer [wcar]]))

(def server {:pool {}
             :spec {:host "127.0.0.1" :port 6379}})

(def counter-key "encogio.counter")
(def id-prefix "encogio.id:")

(defn unique-id
  [conn]
  (enc/base-encode (wcar conn (car/incr counter-key)) enc/default-alphabet))

(defn key-exists?
  [conn k]
  (= 1 (wcar conn (car/exists k))))

(defn set-key!
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

(defn store-url!
  ([conn url]
   (store-url! conn url (unique-id conn)))
  ([conn url id]
   (let [id-key (str id-prefix id)
         result (set-key! conn id-key url)]
     (if (:encogio.anomalies/category result)
       (recur conn url (unique-id conn))
       {:url (:value result)
        :id id}))))
  
(defn alias-url!
  [conn url id]
  (let [id-key (str id-prefix id)
        result (set-key! conn id-key url)]
    (if (:encogio.anomalies/category result)
      result
      {:url (:value result)
       :id id})))

#_(defn id-taken?
  [conn id]
  (not (nil?
        (wcar conn
          (car/get (str id-prefix id))))))

(defn get-url!
  [conn id]
  (wcar conn
    (car/get (str id-prefix id))))

