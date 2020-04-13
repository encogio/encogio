(ns encogio.api
  (:require
   [encogio.core :as enc]
   [encogio.http :as http]
   [encogio.url :as url]
   [encogio.config :as config]
   [encogio.redis :as redis]
   [encogio.auth :as auth]
   [encogio.admin :as admin]
   [encogio.anomalies :as an]
   [reitit.ring :as ring]
   [reitit.swagger :as swag]
   [reitit.swagger-ui :as swagger-ui]
   [reitit.middleware :as mid]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as params]
   [reitit.coercion.malli :as schema]
   [ring.util.response :as resp]
   [malli.util :as mu]
   [muuntaja.core :as m]))

(defn shorten-handler
  [conn url]
  (let [result (redis/store-url! conn url)]
    (if (an/conflict? result)
      {:status 500}
      {:status 200
       :body {:url url
              :alias (:id result)
              :short-url (url/urlize config/site (:id result))}})))

(defn alias-handler
  [conn url alias]
  (let [result (redis/alias-url! conn url alias)]
    (if (an/conflict? result)
      {:status 409}
      {:status 200
       :body {:url url
              :alias alias
              :short-url (url/urlize config/site (:id result))}})))

(defn shorten
  [conn req]
  (let [{:keys [url alias]} (:body-params req)]
    (if-let [valid-url (url/validate url)]
      (let [host (.getHost valid-url)
            url-str (.toString valid-url)]
        (if (= host (:host config/site))
          {:status 403}
          (if alias
            (if (enc/valid-word? alias)
              (alias-handler conn url-str alias)
              {:status 400
               :body {:code "invalid-alias"
                      :cause "Invalid alias"}})
            (shorten-handler conn url-str))))
      {:status 400
       :body {:code "invalid-url"
              :cause "Invalid URL"}})))

(defn redirect-handler
  [conn id]
  (if-let [url (redis/get-url! conn id)]
    (resp/redirect url :permanent-redirect)
    {:status 404}))

(defn redirect
  [conn {:keys [path-params]}]
  (let [id (:id path-params)]
    (if (enc/valid-word? id)
      (redirect-handler conn id)
      {:status 404})))

(def content-negotiation
  (m/create
   (m/select-formats
    m/default-options
    ["application/json"])))

(def content-negotiation-middleware
  [muuntaja/format-negotiate-middleware
   muuntaja/format-response-middleware
   muuntaja/format-request-middleware])

(defn rate-limit-middleware
  [conn settings]
  (mid/map->Middleware
   {:name ::rate-limit
    :description "Middleware that rate limits by IP"
    :wrap (fn [handler]
            (fn [request]
              (if-let [ip (http/request->ip request)]
                (let [[limit ttl] (redis/rate-limit conn settings ip)]
                  (if (= limit :limit)
                    {:status 429
                     :headers {"Retry-After" (str ttl)}}
                    (handler request)))
                (handler request))))}))

(def shorten-request
  [:map
   [:url uri?]
   [:alias
    {:optional true}
    enc/alphabet-regex]])

(def shorten-response
  [:map
   [:url  uri?]
   [:alias enc/alphabet-regex]
   [:short-url uri?]])

(def shorten-error
  [:map
   [:code
    [:enum
     "invalid-url"
     "invalid-alias"]]])

(def base-path (url/site-root config/site))

(def swagger-config
  {:info {:title "URL shortener API"
          :description "The API uses JWT authentication. If you are interested in getting a token please [get in touch](mailto:bandarra@protonmail.com)."}
   :basePath (:host config/site)
   :securityDefinitions {:jwt {:type "apiKey"
                               :in "header"
                               :name "Authorization"}}})

(def swagger-ui-config
  {:path "/api/docs/"
   :url "/api/swagger.json"})

;; todo: pass site config as param
(defn routes
  [conn]
  [["/api" {:muuntaja content-negotiation
            :middleware [muuntaja/format-negotiate-middleware
                         muuntaja/format-response-middleware
                         muuntaja/format-request-middleware
                         (rate-limit-middleware conn config/rate-limit)
                         swag/swagger-feature]}
    ;; docs
    ["/swagger.json"
     {:get {:no-doc true
            :swagger swagger-config
            :handler (swag/create-swagger-handler)}}]
    ["/docs/*" {:no-doc true
                :get (swagger-ui/create-swagger-ui-handler swagger-ui-config)}]
    ;; API
    ["/shorten" {:swagger {:tags ["urls"]
                           :name "Shorten"
                           :description "Shorten a URL, optionally giving it an alias."}
                 :post {:middleware auth/auth-middleware
                        :coercion (schema/create
                                   {;; set of keys to include in error messages
                                    :error-keys #{#_:type :coercion :in :schema :value :errors :humanized #_:transformed}
                                    ;; schema identity function (default: close all map schemas)
                                    :compile mu/closed-schema
                                    ;; strip-extra-keys (effects only predefined transformers)
                                    :strip-extra-keys true
                                    ;; add/set default values
                                    :default-values true
                                    ;; malli options
                                    :options nil})
                        :parameters {:body shorten-request}
                        :responses {200 {:body shorten-response}
                                    500 {:description "Server error"}
                                    401 {:description "Authentication error"}
                                    403 {:description "The specified is not allowed to be shortened"}
                                    409 {:description "The specified alias is taken"}
                                    400 {:description "Either the specified URL or alias is not valid"
                                         :body shorten-error}
                                    429 {:description "You have reached the rate limit"}}
                        :handler #(shorten conn %)}}]]
   ;; Redirections
   ["/:id" {:get #(redirect conn %) :no-doc true}]])
