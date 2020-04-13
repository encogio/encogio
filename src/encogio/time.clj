(ns encogio.time
  (:require
   [clojure.string :as string]))

(def minute 60)
(def hour (* 60 minute))
(def day (* 24 hour))
(def week (* 7 day))

(defn seconds->duration
  [seconds]
  (let [weeks   ((juxt quot rem) seconds week)
        wk      (first weeks)
        days    ((juxt quot rem) (last weeks) day)
        d       (first days)
        hours   ((juxt quot rem) (last days) hour)
        hr      (first hours)
        min     (quot (last hours) minute)
        sec     (rem (last hours) minute)]
    (string/join ", "
                 (filter #(not (string/blank? %))
                         (conj []
                               (when (> wk 0) (str wk " week"))
                               (when (> d 0) (str d " day"))
                               (when (> hr 0) (str hr " hour"))
                               (when (> min 0) (str min " min")))))))

(defn seconds->unit
  [seconds]
  (let [weeks   ((juxt quot rem) seconds week)
        wk      (first weeks)
        days    ((juxt quot rem) (last weeks) day)
        d       (first days)
        hours   ((juxt quot rem) (last days) hour)
        hr      (first hours)
        min     (quot (last hours) minute)
        sec     (rem (last hours) minute)]
    (string/join ", "
                 (filter #(not (string/blank? %))
                         (conj []
                               (when (= wk 1) "week")
                               (when (= d 1) "day")
                               (when (= hr 1) "hour")
                               (when (= min 1) "min")
                               (when (= sec 1) "sec"))))))
