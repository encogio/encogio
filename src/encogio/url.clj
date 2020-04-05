(ns encogio.url
  (:import [java.net URL MalformedURLException]))

;; todo: better uri handling, add http scheme if absent

(def site-host "encog.io")

(defn validate
  [s]
  (try
    (let [parsed (URL. s)]
      (when (not= (.getHost parsed) site-host)
        (.toString parsed)))
    (catch MalformedURLException e)))
