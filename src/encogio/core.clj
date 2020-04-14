(ns encogio.core)

(set! *warn-on-reflection* true)

(def ^String alphabet
  "abcdefghijklmnñopqrstuvwxyzABCDEFGHIJKLMNÑOPQRSTUVWXYZ1234567890_-")

(def base (count alphabet))

(def alphabet-regex
  #"[abcdefghijklmnñopqrstuvwxyzABCDEFGHIJKLMNÑOPQRSTUVWXYZ1234567890_-]+")

(defn valid-word?
  "Check if the given word is valid for our alphabet."
  [s]
  (re-matches alphabet-regex s))

(defn base-encode
  [input]
  (loop [n input
         res ""]
    (cond
      (zero? input) (subs alphabet 0 1)
      (zero? n) res
      :else
      (recur (quot n base)
             (str (nth alphabet (rem n base)) res)))))

(defn base-decode
  [^String input]
  (reduce +
          (map-indexed (fn [idx c]
                         (* (.indexOf alphabet (int c))
                            (bigint
                             (Math/pow base
                                       (- (count input) idx 1)))))
                       input)))


