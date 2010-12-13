(ns hampshire-nlp.narrative-chains.counting
  (:use [clojure.contrib.combinatorics :only [combinations]]))

(defn find-entity
  "Finds an entity given the entity table for a specific
  sentence and the index of the word in that sentence."
  [entity-table word-idx]
  (first (filter #(not (nil? %))
    (for [e entity-table]
      (if (<= (e :begin) word-idx (e :end))
        (e :eid))))))

(defn count-occurences
  "Given the document-wide entity-table and parses,
  Returns the dep-parses with resolved entities."
  [entity-table parses]
  (for [p parses]
    (if (or (= (:dep p) "agent")
            (= (:dep p) "iobj")
            (= (:dep p) "nsubj")
            (= (:dep p) "dobj"))
      (let [eid (find-entity (filter #(= (:sid %) (:sid p)) entity-table) (Integer. (:w2-i p)))]
        (if (nil? eid)
          p
          (assoc p :eid eid)))
      p)))

(defn inc-count-map
  [c1 c2 m]
  (let [same-eid
          (fn [x] (if (= (:eid c1) (:eid c2))
                     (assoc
                       (assoc x :eid (conj (:eid x) (:eid c1)))
                       :cnt (inc (:cnt x)))
                     x))
        naive-cnt
          (fn [x] (assoc x :naive-cnt (inc (:naive-cnt x))))]
    (-> m
      (naive-cnt)
      (same-eid))))

(defn make-count-map-key-pair
  [count-map parse-combo]
  (let [[c1 c2] parse-combo
        s (try #{(:w1 c1) (:w1 c2)}
            (catch java.lang.IllegalArgumentException _ #{(:w1 c1)}))
        m (if (nil? (count-map s)) {:cnt 0 :eid '() :naive-cnt 0} (count-map s))]
     (assoc count-map s (inc-count-map c1 c2 m))))

(defn make-count-map-key-pair-and-dep
  [count-map parse-combo]
  (let [[c1 c2] parse-combo
        s (try #{{:word (:w1 c1) :dep (:dep c1)} {:word (:w1 c2) :dep (:dep c2)}}
            (catch java.lang.IllegalArgumentException _ #{{:word (:w1 c1) :dep (:dep c1)}}))
        m (if (nil? (count-map s)) {:cnt 0 :eid '() :naive-cnt 0} (count-map s))]
     (assoc count-map s (inc-count-map c1 c2 m))))

(defn make-count-map
  "Makes a count-map given the parses.  The keys of the map are of three types
  depending on the passed key-type keyword.  It can take the values :word,
  :word-pair, or :word-pair-and-dep."
  [parses key-type]
 (let [make-key (cond (= key-type :word) (fn [a b] nil)
                      (= key-type :word-pair) make-count-map-key-pair
                      (= key-type :word-pair-and-dep) make-count-map-key-pair-and-dep)]
   (if (= key-type :word)
     nil ;TODO
     (loop [count-map {}
            combos (combinations (filter :eid parses) 2)]
       (if (seq combos)
         (recur (make-key count-map (first combos))
                (rest combos))
         count-map)))))

(defn merge-two-values
  [w1 w2]
  (let [eid-merge (fn [w] (if (:eid w1) (assoc w :eid (concat (:eid w1) (:eid w2))) w))
        naive-cnt-merge (fn [w] (if (:naive-cnt w1) (assoc w :naive-cnt (+ (:naive-cnt w1) (:naive-cnt w2))) w))]
    (naive-cnt-merge (eid-merge (assoc w1 :cnt (+ (:cnt w1) (:cnt w2)))))))

(defn merge-two-count-maps
  [map1 map2]
  ;make m1 the biggest of the given maps
  (let [m1 (if (> (count map1) (count map2)) map1 map2)
        m2 (if (> (count map1) (count map2)) map2 map1)]
    (loop [m2-keys (keys m2)
           m3 (merge m2 m1)]
      (if (seq m2-keys)
        (recur (rest m2-keys)
               (let [key-in-? (first m2-keys)]
                 (if (nil? (m1 key-in-?))
                   m3
                   (assoc m3 key-in-? (merge-two-values (m1 key-in-?) (m2 key-in-?))))))
        m3))))

(defn merge-count-map-vector
  [count-map-vector]
  (reduce merge-two-count-maps count-map-vector))
