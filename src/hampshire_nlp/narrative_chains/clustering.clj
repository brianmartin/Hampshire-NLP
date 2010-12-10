(ns hampshire-nlp.narrative-chains.clustering)

(defn total-pair-count
  [count-map]
  (apply + (for [e count-map] (:cnt (second e)))))

(defn total-individual-count
  ;TODO probably wrong
  [count-map]
  (* 2 (total-pair-count count-map)))

(defn pair-totals
  [count-map]
  (into {} (for [[k v] count-map] [k (v :cnt)])))

(defn individual-totals
  [count-map]
  (loop [verb-pairs count-map
         ind-cnts {}]
    (if (seq verb-pairs)
      (recur (rest verb-pairs)
             (let [vp (first verb-pairs)
                   words (first vp)
                   vp-cnt ((second vp) :cnt)]
               (if (zero? vp-cnt)
                 ind-cnts
                 (merge-with + ind-cnts 
                   (cond (= 1 (count words)) {(first words) (* 2 vp-cnt)}
                         (= 2 (count words)) {(first words) vp-cnt (second words) vp-cnt})))))
      ind-cnts)))

(defn pair-probabilities
  [count-map]
  (let [pair-cnts (pair-totals count-map)
        total (total-pair-count count-map)]
    (assoc (into {} (for [[k v] pair-cnts] [k (/ v total)]))
           :total total)))

(defn ind-probabilities
  [count-map individual-totals]
  (let [ind-cnts (individual-totals count-map)
        total (total-individual-count count-map)]
    (into {} (for [[k v] ind-cnts] [k (/ v total)]))))

(defn pmi-map
  [count-map]
  (let [pair-probs (pair-probabilities count-map)]
    (into {} (for [[k v] pair-probs] [k (Math/log (/ v (:total pair-probs)))]))))
