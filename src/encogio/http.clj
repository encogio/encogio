(ns encogio.http
  (:require
   [encogio.redis :as redis]
   [clojure.string :refer [trim]]
   [ring.util.response :as resp :refer [resource-response]]
   [reitit.middleware :as mid]))

(defn request->ip
  [request]
  (when-let [forwarded-for (get-in request [:headers "x-forwarded-for"])]
    (trim (re-find #"[^,]*" forwarded-for))))
