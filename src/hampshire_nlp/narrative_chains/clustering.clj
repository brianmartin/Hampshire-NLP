(ns hampshire-nlp.narrative-chains.clustering)

(defn total-count
  [count-map]
  (apply + (for [e count-map] (:cnt (second e)))))

(defn total-ind-and-pair-counts
  [count-map]
  (let [total (total-count count-map)]
    (loop [verb-pairs count-map
           pair-cnts {}
           ind-cnts {}]
      (if (seq verb-pairs)
        (let [verb-pair (first verb-pairs) ;here verb-pair is [#{"v1" "v2"} {:cnt 10}]
              pair-cnt (pair-cnts (first verb-pair))
              v1-cnt (ind-cnts (ffirst verb-pair))
              v2-cnt (try (ind-cnts (second (first (verb-pair))))
                       (catch java.lang.Exception _ :no-v2))]
          (recur (rest verb-pairs)
                 (if (not= 0 ((second verb-pair) :cnt))
                   (if pair-cnt
                     (assoc pair-cnts (first verb-pair) (+ pair-cnt ((second verb-pair) :cnt)))
                     (assoc pair-cnts (first verb-pair) ((second verb-pair) :cnt)))
                   pair-cnts)
                 (if (not= 0 ((second verb-pair) :cnt))
                   (-> ind-cnts
                      (#(assoc % (ffirst verb-pair) (if v1-cnt 
                                                     (+ v1-cnt ((second verb-pair) :cnt))
                                                     ((second verb-pair) :cnt))))
                      (#(if (= v2-cnt :no-v2) 
                         %
                         (assoc % (second (first verb-pair)) (if (number? v2-cnt)
                                                               (+ v2-cnt ((second verb-pair) :cnt))
                                                               ((second verb-pair) :cnt))))))
                   ind-cnts)))
        {:individual-verb-counts ind-cnts
         :verb-pair-counts pair-cnts}))))

(defn joint-p
  [count-map-entry total]
  (assoc count-map-entry :joint-p (/ (:cnt count-map-entry) total)))

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
