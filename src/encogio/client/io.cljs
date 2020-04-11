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
            "Authorization" (str "Bearer " token)}))

(def api-url "/api/shorten")

(defn post-json!
  [body]
  (p/create
   (fn [resolve reject]
     (let [json (js/JSON.stringify body)
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

(defn shorten!
  [url]
  (post-json! #js {:url url}))

(defn alias!
  [url alias]
  (post-json! #js {:url url :alias alias}))


