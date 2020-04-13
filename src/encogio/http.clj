(ns encogio.http
  (:require
   [encogio.redis :as redis]
   [clojure.string :refer [trim]]
   [ring.util.response :as resp :refer [resource-response]]
   [reitit.middleware :as mid]))

(defn home
  [_]
  (resource-response "index.html" {:root "public"}))

(defn request->ip
  [request]
  (when-let [forwarded-for (get-in request [:headers "x-forwarded-for"])]
    (trim (re-find #"[^,]*" forwarded-for))))

(defn rate-limit-middleware
  [conn settings]
  (mid/map->Middleware
   {:name ::rate-limit
    :description "Middleware that rate limits by IP"
    :wrap (fn [handler]
            (fn [request]
              (if-let [ip (request->ip request)]
                (let [[limit ttl] (redis/rate-limit conn settings ip)]
                  (if (= limit :limit)
                    {:status 429
                     :headers {"Retry-After" (str ttl)}}
                    (handler request)))
                (handler request))))}))
