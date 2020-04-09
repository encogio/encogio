(ns encogio.client.io
  (:require
   [promesa.core :as p]
   [clojure.spec.alpha :as s]
   [goog.net.XhrIo :as xhr]))

;; api

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
                          403 (reject :forbidden-domain)                                                  400 (if (= (.-code (.getResponseJson response))
                                     "invalid-alias")
                               (reject :invalid-alias)
                               (reject :invalid-url))
                          409 (reject :used-alias)
                          429 (reject :rate-limit)
                          (reject :network-error))))]
       (xhr/send api-url callback "POST" json headers timeout)))))

;; local storage

(def local-storage js/localStorage)
(def storage-key "encogio.short-urls")

(s/def ::shortened-url (s/keys :req-un [::url
                                        ::short-url]))

(s/def ::url string?)
(s/def ::short-url string?)

(s/def ::storage-format
  (s/coll-of ::shortened-url :into []))

(defn read-shortened-urls!
  []
  (when-let [raw (.getItem js/localStorage storage-key)]
    (try
      (let [result (js->clj (js/JSON.parse raw) :keywordize-keys true)
            conformed (s/conform ::storage-format result)]
        (when-not (= conformed :clojure.spec.alpha/invalid)
          conformed))
    (catch :default e))))

(defn write-shortened-urls!
  ([urls]
   (let [as-str (js/JSON.stringify (clj->js urls))]
     (.setItem js/localStorage storage-key as-str))))
