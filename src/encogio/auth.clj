(ns encogio.auth
  (:require
   [encogio.config :as config]
   [buddy.sign.jwt :as jwt]
   [buddy.auth.middleware :refer [wrap-authentication]]
   [buddy.auth.backends :as backends]))

(def backend
  (backends/jws {:secret config/secret-key
                 :token-name "Bearer"}))

(def jwt-middleware
  {:name ::auth
   :description "JWT Authentication middleware"
   :wrap
   (fn [handler]
     (wrap-authentication handler backend))})

(def identity-middleware
  {:name ::identity
   :wrap
   (fn [handler]
     (wrap-authentication handler backend))})

(defn require-identity
  [handler]
  (fn [request]
    (if (:identity request)
      (handler request)
      {:status 401})))

(defn create-token
  [payload]
  (jwt/sign payload config/secret-key))

(def auth-middleware [jwt-middleware require-identity])
