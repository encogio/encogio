(ns encogio.url
  #?(:clj
     (:import
      [java.net URL MalformedURLException])))

#?(:clj
   (defn validate
     [s]
     (try
       (URL. s)
       (catch MalformedURLException _))))

(defn urlize
  [{:keys [scheme host]} k]
  (str scheme "://" host "/" k))

(defn site-root
  [{:keys [scheme host]}]
  (str scheme "://" host))
