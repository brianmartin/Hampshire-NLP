(ns hampshire-nlp.narrative-chains.counting
  (:use [clojure.contrib.combinatorics :only [combinations]]))

(defn find-entity
  "Finds an entity given the entity table for a specific
  sentence and the index of the word in that sentence."
  [entity-table word-idx]
  (first (filter #(not (nil? %))
    (for [e entity-table]
      (if (<= (e :begin)
              word-idx
              (e :end))
        (e :eid)
        nil)))))

(defn count-occurences
  "Given the document-wide entity-table and parses,
  Returns the dep-parses with resolved entities."
  [entity-table parses]
  (for [p parses]
    (if (or (= (:dep p) "agent")
            (= (:dep p) "iobj")
            (= (:dep p) "nsubj")
            (= (:dep p) "dobj"))
      (let [sid (:sid p)
            eid (find-entity (filter #(= (:sid %) sid) entity-table) (Integer. (:w2-i p)))]
        (println sid eid)
        (if (nil? eid)
          p
          (assoc (assoc p :eid eid) :sid sid)))
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

(defn make-count-map
  [parses]
  (loop [count-map {}
         combos (combinations (filter :eid parses) 2)]
    (if (seq combos)
      (recur (let [combo (first combos)
                   [c1 c2] combo
                   s (try #{(:w1 c1) (:w1 c2)}
                       (catch java.lang.IllegalArgumentException _
                              #{(:w1 c1)}))
                   m (if (nil? (count-map s)) {:cnt 0 :eid '() :naive-cnt 0} (count-map s))]
               (assoc count-map s (inc-count-map c1 c2 m)))
             (rest combos))
      count-map)))

(defn merge-two-values
  [w1 w2]
  (-> w1
    (assoc :cnt (+ (:cnt w1) (:cnt w2)))
    (assoc :eid (concat (:eid w1) (:eid w2)))
    (assoc :naive-cnt (+ (:naive-cnt w1) (:naive-cnt w2)))))

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
