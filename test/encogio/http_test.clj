(ns encogio.http-test
  (:require
   [encogio.redis :as redis]
   [taoensso.carmine :as car :refer [wcar]]
   [muuntaja.core :as m]
   [encogio.auth :as auth]
   [encogio.http :as http :refer [app]]
   [encogio.config :as config]
   [encogio.config :refer [redis-conn]]
   [clojure.test :refer [deftest is]]))

(defn post-shorten!
  ([body]
   (post-shorten! body {:auth? true}))
  ([body {:keys [auth?]}]
   (let [headers {"content-type" "application/json"
                  "accept" "application/json"}
         req  {:request-method :post
               :headers (if auth?
                          (assoc headers
                                 "authorization"
                                 (str "Bearer " (auth/create-token {:user-id :anonymous})))
                          headers)
               :uri "/api/shorten"
               :body (m/encode "application/json" body)}
         resp (app req)]
     (if (= 200 (:status resp))
       (update resp :body #(m/decode "application/json" (slurp %)))
       resp))))

(defn shorten!
  ([url]
   (post-shorten! {:url url}))
  ([url alias]
   (post-shorten! {:url url :alias alias})))

(defn anon-shorten!
  ([url]
   (post-shorten! {:url url}
                  {:auth? false}))
  ([url alias]
   (post-shorten! {:url url
                   :alias alias}
                  {:auth? false})))

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

(deftest shorten-rejects-urls-from-site-domain
  (let [url (str "http://" (:host config/site) "/asdfsad")
        resp (shorten! url)]        
    (is (= (:status resp) 403))))

;; shorten: auth

(deftest shorten-returns-unauthorized-if-no-auth-token
  (let [url "http://google.com"
        resp (anon-shorten! url)]
    (is (= (:status resp) 401))))

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

(deftest shorten-accepts-aliases
  (let [url "http://google.com"
        alias "dont-be-evil"
        _ (wcar redis-conn (car/del (redis/make-id-key alias)))
        resp (shorten! url alias)]
    (is (= (:status resp) 200))
    (is (= (get-in resp [:body :url]) url))
    (is (= (get-in resp [:body :alias]) alias))))

(deftest shorten-rejects-duplicate-aliases
  (let [url "http://facebook.com"
        alias "privacy"
        _ (wcar redis-conn (car/del (redis/make-id-key alias)))
        resp (shorten! url alias)
        err (shorten! url alias)]
    (is (= (:status resp) 200))
    (is (= (:status err) 409))))

(deftest shorten-rejects-invalid-aliases
  (let [url "http://facebook.com"
        alias "not a valid alias"
        _ (wcar redis-conn (car/del (redis/make-id-key alias)))
        resp (shorten! url alias)]
    (is (= (:status resp) 400))))

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
        _ (wcar redis-conn (car/del (redis/make-id-key id)))
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
        _ (wcar redis-conn (car/del (redis/make-rate-limit-key addr)))
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
