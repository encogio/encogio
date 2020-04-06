(ns encogio.http-test
  (:require
   [encogio.redis :as redis]
   [taoensso.carmine :as car :refer [wcar]]
   [encogio.http :as http :refer [app]]
   [encogio.config :refer [redis-conn]]
   [clojure.test :refer [deftest is]]))

(deftest redirect-handler-redirects-to-url-if-match
  (let [url "http://google.com/asdfsad"
        {:keys [id]} (redis/store-url! redis-conn "http://google.com/asdfsad")
    resp (http/redirect-handler redis-conn id)]
    (is (= (:status resp) 302))
    (is (= (get-in resp [:headers "Location"]) url))))

(deftest redirect-handler-return-not-found-if-no-match
  (let [id "not-matching"
        _ (wcar redis-conn (car/del (str redis/id-prefix id)))
        resp (http/redirect-handler redis-conn id)]
    (is (= (:status resp) 404))))

(deftest redirect-returns-not-found-for-illegal-alias
  (let [id "not an alias"
        req {:request-method :get
             :uri (str "/" id)}
        resp (app req)]
    (is (= (:status resp) 404))))

;; api

(deftest shorten-rejects-phrases
  (let [non-url "afsldkjflsakdjfl sadfjlksadj lksjdfslkd"
        req {:request-method :post
             :uri "/api/shorten"
             :body non-url}
        resp (app req)]
    (is (= (:status resp) 400))))

(deftest shorten-rejects-words
  (let [non-url "garbage"
        req {:request-method :post
             :uri "/api/shorten"
             :body non-url}
        resp (app req)]
    (is (= (:status resp) 400))))

(deftest shorten-rejects-non-urls
  (let [non-url "garbage."
        req {:request-method :post
             :uri "/api/shorten"
             :body non-url}
        resp (app req)]
    (is (= (:status resp) 400))))

(deftest shorten-accepts-urls
  (let [url "http://google.com"
        req {:request-method :post
             :uri "/api/shorten/"
             :body url}
        resp (app req)]
    (is (= (:status resp) 200))))

(deftest shorten-accepts-urls-with-paths
  (let [url "http://google.com/asdfsdaf"
        req {:request-method :post
             :uri "/api/shorten/"
             :body url}
        resp (app req)]
    (is (= (:status resp) 200))))

(deftest shorten-accepts-urls-with-query-params
  (let [url "http://google.com/asdfsdaf?query=foo&anotherQuery=bat"
        req {:request-method :post
             :uri "/api/shorten/"
             :body url}
        resp (app req)]
    (is (= (:status resp) 200))))

(deftest shorten-accepts-urls-with-query-params-and-fragment
  (let [url "http://google.com/asdfsdaf?query=foo&anotherQuery=bat#section1"
        req {:request-method :post
             :uri "/api/shorten/"
             :body url}
        resp (app req)]
    (is (= (:status resp) 200))))

(deftest shorten-does-not-accept-urls-without-scheme
  (let [url "google.com"
        req {:request-method :post
             :uri "/api/shorten/"
             :body url}
        resp (app req)]
    (is (= (:status resp) 400))))

(deftest shorten-rejects-urls-from-app-domain
  (let [url "http://encog.io/asdfsad"
        req {:request-method :post
             :uri "/api/shorten/"
             :body url}
        resp (app req)]
    (is (= (:status resp) 400))))
