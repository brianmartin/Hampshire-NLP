(ns hampshire-nlp.narrative-chains.clustering)

(defn total-count
  [count-map]
  (apply + (for [e count-map] (:cnt (second e)))))

(defn pair-totals
  [count-map]
  (apply hash-map (flatten (filter #(not (zero? (second %))) (for [vp count-map] [(first vp) (:cnt (second vp))])))))

(defn individual-totals
  [count-map]
  (loop [verb-pairs count-map
         ind-cnts {}]
    (if (seq verb-pairs)
      (recur (rest verb-pairs)
             (let [vp (first verb-pairs)]
               (if (not= 0 ((second vp) :cnt))
                 (merge-with + ind-cnts 
                   {(ffirst vp) 1}
                   (if (= 2 (count (first verb-pairs)))
                     {(second (first vp)) 1}
                     {}))
                 ind-cnts)))
      ind-cnts)))

(defn pair-probabilities
  [count-map pair-cnts]
  nil)

(defn ind-probabilities
  [count-map pair-cnts]
  nil)

;(defn add-pmi-to-map
; [word-set counts total individual-prob-map]
; (if (= (count word-set) 2)
;   (let [v1 (first word-set)
;         v2 (second word-set)]


;(defn pmi->map
; [count-map]
; (let [ind-and-pair-totals (total-ind-and-pair-counts count-map)
;       pair-probs (pair-probabilites count-map (:pair ind-and-pair-totals))
;       individual-probs (individual-probabilities count-map (:ind ind-and-pair-totals))]
;   (loop [ks (keys count-map)
;          pmi-map {}]
;     (if (seq ks)
;       (recur (rest ks)
;              (let [k (first ks)
;                    entry (k count-map)]
;                (add-pmi-to-map k entry pair-probs individual-probs)))
;       pmi-map))))
