(ns encogio.redis
  (:require
   [encogio.core :as enc]
   [taoensso.carmine :as car :refer [wcar]]))

(def server {:pool {}
             :spec {:host "127.0.0.1" :port 6379}})

(def counter-key "encogio.counter:")
(def url-prefix "encogio.url:")
(def id-prefix "encogio.id:")

(defn unique-id
  [conn]
  (wcar conn (car/incr counter-key)))

(defn shorten!
  [conn url]
  (let [pk (unique-id conn)
        id (enc/base-encode pk enc/default-alphabet)
        id-key (str id-prefix id)
        url-key (str url-prefix url)]
    (wcar conn
      #_(car/watch url-key id-key)
      (car/multi)
      (car/set url-key id)
      (car/set id-key url)
      (car/exec))
    {:url url
     :id id}))

#_(defn alias
  [conn url id]
  (wcar conn
    (car/multi)
    (car/set (str url-key url) id)
    (car/set (str base-key id) url)
    (car/exec))
  id)

(defn id-taken?
  [conn id]
  (not (nil?
        (wcar conn
          (car/get (str id-prefix id))))))

(defn expand!
  [conn id]
  (wcar conn
    (car/get (str id-prefix id))))

(comment
  (shorten! server "https://google.com/foo")
  (expand! server "bl")
  
  (alias server "https://google.com" "espia")
  (expand server "espia")

  (expand "L")
  
  (expand "F")
  
  )


