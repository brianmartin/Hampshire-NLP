(ns hampshire-nlp.narrative-chains.clustering)

(defn total-count
  [count-map]
  (apply + (for [e count-map] (:cnt (second e)))))

(defn total-ind-and-pair-count
  [count-map]
  (let [total (total-count count-map)]
    (loop [es count-map
           pair-probs {}
           ind-probs {}]
      (if (seq es)
        (let [e (first es)]
          (recur (rest es)

(defn joint-p
  [count-map-entry total]
  (assoc count-map-entry :joint-p (/ (:cnt count-map-entry) total)))

(defn add-pmi-to-map
  [word-set counts total individual-prob-map]
  (if (= (count word-set) 2)
    (let [v1 (first word-set)
          v2 (second word-set)]

(defn pmi->map
  [count-map]
  (let [ind-and-pair-totals (total-ind-and-pair-count count-map)
        pair-probs (pair-probabilites count-map (:pair ind-and-pair-totals))
        individual-probs (individual-probabilities count-map (:ind ind-and-pair-totals))]
    (loop [ks (keys count-map)
           pmi-map {}]
      (if (seq ks)
        (recur (rest ks)
               (let [k (first ks)
                     entry (k count-map)]
                 (add-pmi-to-map k entry pair-probs individual-probs)))
        pmi-map))))
