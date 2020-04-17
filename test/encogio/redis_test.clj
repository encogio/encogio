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

(defn flush!
  ([]
   (flush! test-server))
  ([conn]
   (wcar conn (car/flushall))))

(def gen-url
  (gen/let [protocol (gen/elements ["http" "https"])
            host (gen/elements ["google.com" "facebook.com" "apple.com"])
            port (gen/choose 80 8080)
            path (gen/fmap #(str "/" %) gen/string-alphanumeric)]
    (gen/return (URL. protocol host port path))))

(deftest redis-is-healthy
  (is (redis/healthy? test-server)))

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
          _ (flush!)
          result (redis/alias-url! test-server u a)
          expanded (redis/get-url! test-server a)]
      (= u expanded))))

(defspec existing-aliases-cant-be-reused
  1000
  (prop/for-all [a (gen/not-empty gen/string-alphanumeric)
                 url gen-url]
    (let [u (.toString url)
          _ (flush!)
          _ (redis/alias-url! test-server u a)
          conflict (redis/alias-url! test-server u a)]
      (= :encogio.anomalies/conflict
         (:encogio.anomalies/category conflict)))))

;; queries

(deftest redis-query-count-urls
  (let [_ (flush!)
        urls (redis/count-urls test-server)]
    (is (zero? urls))
    (redis/store-url! test-server "http://google.com" "goog")
    (redis/store-url! test-server "http://facebook.com" "fb")
    (is (= 2 (redis/count-urls test-server)))))




