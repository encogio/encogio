(ns encogio.core)

(set! *warn-on-reflection* true)

(def default-alphabet
  "abcdefghijklmnñopqrstuvwxyzABCDEFGHIJKLMNÑOPQRSTUVWXYZ1234567890_-")
(def alphabet-regex
  #"[abcdefghijklmnñopqrstuvwxyzABCDEFGHIJKLMNÑOPQRSTUVWXYZ1234567890_-]+")
(def default-separators
  "cfhistuCFHISTU")

(defn base-encode
  "Encode a positive number using the provided alphabet."
  ([input ^String alphabet]
   (base-encode input alphabet (bigint input) ""))
  ([input ^String alphabet n ^String res]
   (cond
     (zero? input) (subs alphabet 0 1)
     (zero? n) res
     :else (recur input
                  alphabet
                  (bigint (/ n (count alphabet)))
                  (str (nth alphabet (mod n (count alphabet))) res)))))

(defn base-decode
  "Decode a string into a positive number using the provided alphabet."  
  [^String input ^String alphabet]
  (reduce +
          (map-indexed (fn [idx c]
                         (* (.indexOf alphabet (int c))
                            (bigint
                             (Math/pow (count alphabet)
                                       (- (count input) idx 1)))))
                       input)))


