(ns encogio.time
  (:require
   [clojure.string :as string]))

(def minute 60)
(def hour (* 60 minute))
(def day (* 24 hour))
(def week (* 7 day))

(defn seconds->duration
  [seconds tr]
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
                               (when (> wk 0)
                                 (str wk " " (tr [:time/week] [wk])))
                               (when (> d 0)
                                 (str d " " (tr [:time/day] [d])))
                               (when (> hr 0)
                                 (str hr " " (tr [:time/hour] [hr])))
                               (when (> min 0)
                                 (str min " " (tr [:time/min] [min]))))))))

(defn seconds->unit
  [seconds tr]
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
                               (when (= wk 1) (tr [:time/week] [1]))
                               (when (= d 1) (tr [:time/day] [1]))
                               (when (= hr 1) (tr [:time/hour] [1]))
                               (when (= min 1) (tr [:time/min] [1]))
                               (when (= sec 1) (tr [:time/sec] [1])))))))
