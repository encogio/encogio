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
  "Encode a positive number using the provided alphabet."
  ([input]
   (base-encode input (bigint input) ""))
  ([input n ^String res]
   (cond
     (zero? input) (subs alphabet 0 1)
     (zero? n) res
     :else (recur input
                  (bigint (/ n base))
                  (str (nth alphabet (mod n base)) res)))))

(defn base-decode
  "Decode a string into a positive number using the provided alphabet."  
  [^String input]
  (reduce +
          (map-indexed (fn [idx c]
                         (* (.indexOf alphabet (int c))
                            (bigint
                             (Math/pow base
                                       (- (count input) idx 1)))))
                       input)))


