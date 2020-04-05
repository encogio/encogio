(ns encogio.core
  (:require [clojure.spec.alpha :as s]))

(set! *warn-on-reflection* true)

(def ^String alphabet
  "abcdefghijklmnñopqrstuvwxyzABCDEFGHIJKLMNÑOPQRSTUVWXYZ1234567890_-")
(def alphabet-regex
  #"[abcdefghijklmnñopqrstuvwxyzABCDEFGHIJKLMNÑOPQRSTUVWXYZ1234567890_-]+")

(s/def ::word #(re-matches alphabet-regex %))

(defn valid-word?
  "Check if the given word is valid for our alphabet."
  [s]
  (re-matches alphabet-regex s))

(defn base-encode
  "Encode a positive number using the provided alphabet."
  ([input]
   (base-encode input (bigint input) ""))
  ([input ^String n ^String res]
   (cond
     (zero? input) (subs alphabet 0 1)
     (zero? n) res
     :else (recur input
                  (bigint (/ n (count alphabet)))
                  (str (nth alphabet (mod n (count alphabet))) res)))))

(defn base-decode
  "Decode a string into a positive number using the provided alphabet."  
  [^String input]
  (reduce +
          (map-indexed (fn [idx c]
                         (* (.indexOf alphabet (int c))
                            (bigint
                             (Math/pow (count alphabet)
                                       (- (count input) idx 1)))))
                       input)))


