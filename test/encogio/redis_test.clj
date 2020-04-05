(ns encogio.redis-test
  (:require
   [clojure.test.check.properties :as prop]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.clojure-test :refer [defspec]]
   [clojure.test :refer [deftest is]]
   [encogio.redis :as redis]
   [taoensso.carmine :as car :refer [wcar]])
  (:import [java.net URL]))

(def test-server {:pool {} :spec {:url "127.0.0.0.1"}})

(def gen-url
  (gen/let [protocol (gen/elements ["http" "https"])
            host (gen/elements ["google.com" "facebook.com" "apple.com"])
            port (gen/elements [80 8080])
            path (gen/fmap #(str "/" %) gen/string-alphanumeric)]
    (gen/return (URL. protocol host port path))))

(deftest redis-is-healthy
  (is (= "PONG" (wcar test-server (car/ping)))))

(defspec stored-urls-can-be-expanded-by-their-id
  1000
  (prop/for-all [url gen-url]
    (let [u (.toString url)
          result (redis/store-url! test-server u)
          expanded (redis/get-url! test-server (:id result))]
      (= u expanded))))

(defspec urls-can-be-aliased
  1000
  (prop/for-all [a (gen/not-empty gen/string-alphanumeric)
                 url gen-url]
    (let [u (.toString url)
          _ (wcar test-server (car/del (str redis/id-prefix a)))
          result (redis/alias-url! test-server u a)
          expanded (redis/get-url! test-server a)]
      (= u expanded))))

(defspec existing-aliases-cant-be-reused
  1000
  (prop/for-all [a (gen/not-empty gen/string-alphanumeric)
                 url gen-url]
    (let [u (.toString url)
          _ (wcar test-server (car/del (str redis/id-prefix a)))
          _ (redis/alias-url! test-server u a)
          conflict (redis/alias-url! test-server u a)]
      (= :encogio.anomalies/conflict
         (:encogio.anomalies/category conflict)))))

(comment
  (clojure.test/run-tests 'encogio.redis-test)
  )

