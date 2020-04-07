(ns encogio.client.io
  (:require
   [goog.events :refer [listen unlisten]]
   [promesa.core :as p])
  (:import [goog.net XhrIo EventType]))

;; io

(def json-headers
  (clj->js {"Accept" "application/json"
            "Content-Type" "application/json"}))

(defn shorten!
  [url]
  (p/create
   (fn [resolve reject]
     (let [body (js/JSON.stringify #js {:url url})
           request (XhrIo.)]
       (listen request
               EventType.COMPLETE
               (fn [event]
                 (let [response (-> event .-target)
                       status (.getStatus response)]
                   (case status
                     200
                     (let [body (.getResponseJson response)
                           shortened {:url (.-url body)
                                      :short-url (aget body "short-url")}]
                       (resolve shortened))
                     500 (reject :server-error)
                     400 (reject :invalid-url)
                     429 (reject :rate-limit)
                     (reject :unknown-error)))))
       (listen request
               EventType.ERROR
               (fn [err] ;; ErrorCode.{ TIMEOUT, EXCEPTION, HTTP_ERROR, ABORT }
                 (reject :network-error)))
       (.send request
              "/api/shorten"
              "POST"
              body
              json-headers)))))
