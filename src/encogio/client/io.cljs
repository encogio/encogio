(ns encogio.client.io
  (:require
   [promesa.core :as p]
   [clojure.spec.alpha :as s]
   [goog.net.XhrIo :as xhr])
  (:require-macros
   [encogio.client.token :refer [deftoken]]))

;; api

(def timeout 30000)

(deftoken token)

(def headers
  (clj->js {"Accept" "application/json"
            "Content-Type" "application/json"
            "Authorization" (str "Token " token)}))

(def api-url "/api/shorten")

(defn shorten!
  [url]
  (p/create
   (fn [resolve reject]
     (let [body #js {:url url}
           json (js/JSON.stringify body)
           callback (fn [event]
                      (let [response (-> event .-target)]
                        (case (.getStatus response)
                          200
                          (let [resp (.getResponseJson response)]
                            (resolve {:url (.-url resp)
                                      :short-url (aget resp "short-url")}))
                          500 (reject :server-error)
                          403 (reject :forbidden-domain)
                          400 (reject :invalid-url)
                          429 (reject :rate-limit)
                          (reject :network-error))))]
       (xhr/send api-url callback "POST" json headers timeout)))))

(defn alias!
  [url alias]
  (p/create
   (fn [resolve reject]
     (let [body #js {:url url :alias alias}
           json (js/JSON.stringify body)
           callback (fn [event]
                      (let [response (-> event .-target)]
                        (case (.getStatus response)
                          200
                          (let [resp (.getResponseJson response)]
                            (resolve {:url (.-url resp)
                                      :short-url (aget resp "short-url")}))
                          500 (reject :server-error)
                          403 (reject :forbidden-domain)
                          400 (if (= (.-code (.getResponseJson response))
                                     "invalid-alias")
                                (reject :invalid-alias)
                                (reject :invalid-url))
                          409 (reject :used-alias)
                          429 (reject :rate-limit)
                          (reject :network-error))))]
       (xhr/send api-url callback "POST" json headers timeout)))))

(s/def ::shortened-url (s/keys :req-un [::url ::short-url]))

(s/def ::url string?)
(s/def ::short-url string?)


