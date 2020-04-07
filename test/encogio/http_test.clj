(ns encogio.http-test
  (:require
   [encogio.redis :as redis]
   [taoensso.carmine :as car :refer [wcar]]
   [encogio.http :as http :refer [app]]
   [encogio.config :refer [redis-conn]]
   [clojure.test :refer [deftest is]]))

(defn shorten!
  [url]
  (let [req
        {:request-method :post
         :headers {"content-type" "application/edn"
                   "accept" "application/edn"}
         :uri "/api/shorten"
         :body (pr-str {:url url})}
        resp (app req)]
   (if (= 200 (:status resp))
     (update resp :body #(clojure.edn/read-string (slurp %)))
     resp)))

;; shorten: invalid URLs

(deftest shorten-rejects-phrases
  (let [non-url "afsldkjflsakdjfl sadfjlksadj lksjdfslkd"
        resp (shorten! non-url)]
    (is (= (:status resp) 400))))

(deftest shorten-rejects-words
  (let [non-url "garbage"
        resp (shorten! non-url)]
    (is (= (:status resp) 400))))

(deftest shorten-rejects-non-urls
  (let [non-url "garbage."
        resp (shorten! non-url)]
    (is (= (:status resp) 400))))

(deftest shorten-rejects-urls-without-scheme
  (let [url "google.com"
        resp (shorten! url)]        
    (is (= (:status resp) 400))))

(deftest shorten-rejects-urls-from-app-domain
  (let [url "http://encog.io/asdfsad"
        resp (shorten! url)]        
    (is (= (:status resp) 400))))

;; shorten: accepted URLs

(deftest shorten-accepts-urls
  (let [url "http://google.com"
        resp (shorten! url)]
    (is (= (:status resp) 200))
    (is (= (get-in resp [:body :url]) url))))

(deftest shorten-accepts-urls-with-paths
  (let [url "http://google.com/asdfsdaf"
        resp (shorten! url)]
    (is (= (:status resp) 200))
    (is (= (get-in resp [:body :url]) url))))

(deftest shorten-accepts-urls-with-query-params
    (let [url "http://google.com/asdfsdaf?query=foo&anotherQuery=bat"
          resp (shorten! url)]
      (is (= (:status resp) 200))
      (is (= (get-in resp [:body :url]) url))))


(deftest shorten-accepts-urls-with-query-params-and-fragment
  (let [url "http://google.com/asdfsdaf?query=foo&anotherQuery=bat#section1"
        resp (shorten! url)]
    (is (= (:status resp) 200))
    (is (= (get-in resp [:body :url]) url))))

;; redirection

(deftest redirect-handler-redirects-to-url-if-match
  (let [url "http://google.com/asdfsad"
        {:keys [id]} (redis/store-url! redis-conn "http://google.com/asdfsad")
        req {:request-method :get
             :uri (str "/" id)}
        resp (app req)]
    (is (= (:status resp) 308))
    (is (= (get-in resp [:headers "Location"]) url))))

(deftest redirect-handler-return-not-found-if-no-match
  (let [id "not-matching"
        _ (wcar redis-conn (car/del (str redis/id-prefix id)))
        req {:request-method :get
             :uri (str "/" id)}        
        resp (app req)]
    (is (= (:status resp) 404))))

(deftest redirect-returns-not-found-for-illegal-alias
  (let [id "not an alias"
        req {:request-method :get
             :uri (str "/" id)}
        resp (app req)]
    (is (= (:status resp) 404))))

;; rate limit

(deftest rate-limit-limits-by-remote-address
  (let [addr "123.123.1.1"
        _ (wcar redis-conn (car/del (str redis/rate-limit-prefix addr)))
        {:keys [wrap]} (http/rate-limit-middleware redis-conn
                                                   {:limit 2
                                                    :limit-duration 3600})
        handler (wrap (constantly {:status 200}))
        req {:headers {"x-forwarded-for" addr}}]
    (is (= 200 (:status (handler req))))
    (is (= 200 (:status (handler req))))
    (is (= 429 (:status (handler req))))))

(deftest rate-limit-limits-by-proxies-remote-address
  (let [addr "123.123.1.1"
        _ (wcar redis-conn (car/del (str redis/rate-limit-prefix addr)))
        {:keys [wrap]} (http/rate-limit-middleware redis-conn
                                                   {:limit 2
                                                    :limit-duration 3600})
        handler (wrap (constantly {:status 200}))
        req {:headers {"x-forwarded-for" (str addr ", 122.131.4.1")}}]
    (is (= 200 (:status (handler req))))
    (is (= 200 (:status (handler req))))
    (is (= 429 (:status (handler req))))))
