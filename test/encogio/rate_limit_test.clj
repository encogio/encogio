(ns encogio.rate-limit-test
  (:require
   [clojure.test :refer [deftest is]]
   [encogio.redis-test :refer [flush! test-server]]
   [encogio.redis.rate-limit :as rl]
   [taoensso.carmine :as car :refer [wcar]]))

(deftest rate-limit-limits-after-all-attempts-consumed
  (let [key "rate-limited"
        config {:limit 1 :limit-duration 60}
        _ (flush!)
        _ (is (not (rl/limited? test-server config key)))
        [ok rem] (rl/rate-limit test-server config key)]
    (is (= ok :ok))
    (is (rl/limited? test-server config key))
    (is (= 0 rem))
    (let [[err ttl] (rl/rate-limit test-server config key)]
      (is (= err :limit))
      (is (rl/limited? test-server config key))
      (is (= 60 ttl)))))

;; queries

(deftest redis-query-count-rate-limits
  (let [_ (flush!)
        config {:limit 10 :limit-duration 60}
        client1 "a-client"
        client2 "another-client"
        clients (rl/count-rate-limits test-server)]
    (is (zero? clients))
    (rl/rate-limit test-server config client1)
    (rl/rate-limit test-server config client2)
    (is (= 2 (rl/count-rate-limits test-server)))
    (rl/rate-limit test-server config client1)
    (rl/rate-limit test-server config client2)
    (is (= 2 (rl/count-rate-limits test-server)))))

(deftest redis-query-get-rate-limits
  (let [_ (flush!)
        config {:limit 10 :limit-duration 60}
        client1 "a-client"
        client2 "another-client"
        clients (rl/get-rate-limits test-server)]
    (is (empty? clients))
    (rl/rate-limit test-server config client1)
    (is (= #{[client1 {:hits 1 :ttl 60}]}
           (rl/get-rate-limits test-server)))
    (rl/rate-limit test-server config client2)
    (is (= #{[client1 {:hits 1 :ttl 60}]
             [client2 {:hits 1 :ttl 60}]}
           (rl/get-rate-limits test-server)))
    (rl/rate-limit test-server config client2)
    (is (= #{[client1 {:hits 1 :ttl 60}]
             [client2 {:hits 2 :ttl 60}]}
           (rl/get-rate-limits test-server)))
    (rl/rate-limit test-server config client1)
    (is (= #{[client1 {:hits 2 :ttl 60}]
             [client2 {:hits 2 :ttl 60}]}
           (rl/get-rate-limits test-server)))))

(deftest redis-query-get-rate-limits-for-custom-prefix
  (let [_ (flush!)
        prefix "admin.login-attempts:"
        pattern "admin.login-attempts:*"
        config {:limit 10
                :limit-duration 60
                :prefix prefix}
        client1 "a-client"
        client2 "another-client"
        clients (rl/get-rate-limits test-server pattern prefix)]
    (is (empty? clients))
    (rl/rate-limit test-server config client1)
    (is (= #{[client1 {:hits 1 :ttl 60}]}
           (rl/get-rate-limits test-server pattern prefix)))
    (rl/rate-limit test-server config client2)
    (is (= #{[client1 {:hits 1 :ttl 60}]
             [client2 {:hits 1 :ttl 60}]}
           (rl/get-rate-limits test-server pattern prefix)))
    (rl/rate-limit test-server config client2)
    (is (= #{[client1 {:hits 1 :ttl 60}]
             [client2 {:hits 2 :ttl 60}]}
           (rl/get-rate-limits test-server pattern prefix)))
    (rl/rate-limit test-server config client1)
    (is (= #{[client1 {:hits 2 :ttl 60}]
             [client2 {:hits 2 :ttl 60}]}
           (rl/get-rate-limits test-server pattern prefix)))))
