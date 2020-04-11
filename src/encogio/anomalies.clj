(ns encogio.anomalies
  "Based on https://github.com/cognitect-labs/anomalies"
  (:require [clojure.spec.alpha :as s]))

(s/def ::category #{::conflict})
(s/def ::message string?)
(s/def ::anomaly (s/keys :req [::category]
                         :opt [::message]))

(defn conflict
  ([]
   {::category ::conflict})
  ([msg]
   {::category ::conflict
    ::message msg}))

(defn conflict?
  [m]
  (= (::category m) ::conflict))
