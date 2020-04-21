(ns encogio.http
  (:require
   [clojure.string :refer [trim]]))

(defn request->ip
  [request]
  (when-let [forwarded-for (get-in request [:headers "x-forwarded-for"])]
    (trim (re-find #"[^,]*" forwarded-for))))
