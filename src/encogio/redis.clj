(ns encogio.redis
  (:require
   [encogio.core :as enc]
   [taoensso.carmine :as car :refer [wcar]]))

(defn healthy?
  [conn]
  (try
    (= "PONG" (wcar conn (car/ping)))
    (catch Exception _ false)))

(defn- set-new-key!
  [conn k v]
  (let [set?
        (wcar conn
          (car/eval*
   "local exists;
    exists = redis.call('exists', KEYS[1]);
    if tonumber(exists) == 0 then
      return redis.call('set', KEYS[1], KEYS[2]);
    else
      return redis.status_reply('duplicate key');
    end;"
   2 k v))]
    (if (= set? "OK")
      {:key k :value v}
      {:encogio.anomalies/category
       :encogio.anomalies/conflict})))

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
     (if (:encogio.anomalies/category result)
       (recur conn url (unique-id conn))
       {:url (:value result)
        :id id}))))
  
(defn alias-url!
  [conn url id]
  (let [id-key (make-id-key id)
        result (set-new-key! conn id-key url)]
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
        [limit? remaining]
        (wcar conn
          (car/eval*
   "local current;
    local limit;

    current = tonumber(redis.call('llen', KEYS[1]));
    limit = tonumber(KEYS[2]);

    if current >= limit then
      return {'ERROR', 'rate limit'};
    else
        if tonumber(redis.call('exists', KEYS[1])) == 0 then
          redis.call('rpush', KEYS[1], KEYS[1]);
          redis.call('expire', KEYS[1], KEYS[3]);
          return {'OK', limit - 1};
        else
          redis.call('rpushx', KEYS[1], KEYS[1]);
          return {'OK', limit - current - 1};
        end;
    end;"
   3
   key limit limit-duration))]
    (if (= limit? "OK")
      {:remaining remaining}
      :limit)))
