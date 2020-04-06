(ns encogio.url
  (:import [java.net URL MalformedURLException]))

(def site-scheme "http")
(def site-host "encog.io")

(defn validate
  [s]
  (try
    (let [parsed (URL. s)]
      (when (not= (.getHost parsed) site-host)
        (.toString parsed)))
    (catch MalformedURLException _)))

(defn urlize
  [k]
  (str site-scheme "://" site-host "/" k))
