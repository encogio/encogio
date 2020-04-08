(ns encogio.url
  (:require [encogio.config :as config])
  (:import [java.net URL MalformedURLException]))

(defn validate
  [s]
  (try
    (let [parsed (URL. s)]
      (when (not= (.getHost parsed) (:host config/site))
        (.toString parsed)))
    (catch MalformedURLException _)))

(defn urlize
  ([k]
   (urlize config/site k))
  ([{:keys [scheme host]} k]
    (str scheme "://" host "/" k)))
