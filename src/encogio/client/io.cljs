(ns encogio.client.io
  (:require
   [promesa.core :as p]
   [goog.net.XhrIo :as xhr]))

(def timeout 30000)

(def headers
  (clj->js {"Accept" "application/json"
            "Content-Type" "application/json"}))

(def api-url "/api/shorten")

(defn shorten!
  [url]
  (p/create
   (fn [resolve reject]
     (let [body (js/JSON.stringify #js {:url url})
           callback (fn [event]
                      (let [response (-> event .-target)]
                        (case (.getStatus response)
                          200
                          (let [body (.getResponseJson response)]
                            (resolve {:url (.-url body)
                                      :short-url (aget body "short-url")}))
                          500 (reject :server-error)
                          400 (reject :invalid-url)
                          429 (reject :rate-limit)
                          (reject :network-error))))]
       (xhr/send api-url callback "POST" body headers timeout)))))
