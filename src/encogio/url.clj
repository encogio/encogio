(ns encogio.url
  (:import [java.net URL MalformedURLException]))

(defn validate
  [s]
  (try
    (URL. s)
    (catch MalformedURLException _)))

(defn urlize
  [{:keys [scheme host]} k]
  (str scheme "://" host "/" k))
