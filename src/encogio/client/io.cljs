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
                          400 (if (= (.-code (.getResponseJson response))
                                     "invalid-alias")
                               (reject :invalid-alias)
                               (reject :invalid-url))
                          429 (reject :rate-limit)
                          (reject :network-error))))]
       (xhr/send api-url callback "POST" json headers timeout)))))
