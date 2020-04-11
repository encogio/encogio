(ns encogio.http
  (:require
   [encogio.core :as enc]
   [encogio.url :as url]
   [encogio.config :as config]
   [encogio.redis :as redis]
   [encogio.auth :as auth]
   [clojure.string :refer [trim]]
   [ring.util.response :as resp :refer [resource-response]]
   [clojure.spec.alpha :as s]
   [reitit.ring :as ring]
   [reitit.swagger :as swag]
   [reitit.swagger-ui :as swagger-ui]
   [reitit.middleware :as mid]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.coercion.spec :as spec]
   [muuntaja.core :as m]))

(defn shorten-handler
  [conn url]
  (let [result (redis/store-url! conn url)]
    (if (:encogio.anomalies/category result)
      {:status 500}
      {:status 200
       :body {:url url
              :alias (:id result)
              :short-url (url/urlize config/site (:id result))}})))

(defn alias-handler
  [conn url alias]
  (let [result (redis/alias-url! conn url alias)]
    (if (:encogio.anomalies/category result)
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

(defn home
  [_]
  (resource-response "index.html" {:root "public"}))

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
              (if-let [forwarded-for (get-in request [:headers "x-forwarded-for"])]
                (let [remote-addr (trim (re-find #"[^,]*" forwarded-for))
                      limit (redis/rate-limit conn settings remote-addr)]
                  (if (= limit :limit)
                    {:status 429}
                    (handler request)))
                (handler request))))}))

(def api-middleware
  [muuntaja/format-negotiate-middleware
   muuntaja/format-response-middleware
   muuntaja/format-request-middleware
   (rate-limit-middleware config/redis-conn config/rate-limit)
   swag/swagger-feature])

(s/def ::url string?)

(s/def ::alias string?)
(s/def ::short-url string?)

(s/def ::shorten-request (s/keys :req-un [::url]
                                 :opt-un [::alias]))
(s/def ::shorten-response (s/keys :req-un [::url
                                           ::alias
                                           ::short-url]))

(s/def ::shorten-error-code #{"invalid-url" "invalid-alias"})

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

(def router
  (ring/router
   [["" {:get home :no-doc true}]
    ["/" {:get home :no-doc true}]

    ["/api" {:muuntaja content-negotiation
             :middleware api-middleware}
     ;; docs
     ["/swagger.json"
      {:get {:no-doc true
             :swagger swagger-config
             :handler (swag/create-swagger-handler)}}]
     ["/docs/*" {:no-doc true
                 :get (swagger-ui/create-swagger-ui-handler swagger-ui-config)}]
     ;; API
     ["/shorten" {:swagger {:tags ["urls"]}
                  :post {:middleware auth/auth-middleware
                         :coercion spec/coercion
                         :parameters {:body ::shorten-request}
                         :responses {200 {:body ::shorten-response}
                                     500 {:description "Server error"}
                                     401 {:description "Authentication error"}
                                     403 {:description "The specified is not allowed to be shortened"}
                                     409 {:description "The specified alias is taken"}
                                     400 {:description "Either the specified URL or alias is not valid"
                                          :body {:code ::shorten-error-code}}
                                     429 {:description "You have reached the rate limit"}}
                         :handler #(shorten config/redis-conn %)}}]]
    
    ["/:id" {:get #(redirect config/redis-conn %)
             :no-doc true}]]))

(def app
  (ring/ring-handler router
                     (ring/routes
                      (ring/create-resource-handler {:root "public"
                                                     :path "/"})
                      (ring/create-default-handler))))
