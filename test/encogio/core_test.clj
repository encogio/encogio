(ns encogio.core-test
  (:require [clojure.test.check.properties :as prop]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.clojure-test :refer [defspec]]
            [encogio.core :as e]))

(defspec base-encoding-is-bijective
  1000
  (prop/for-all [n gen/nat]
    (= n (e/base-decode (e/base-encode n)))))
