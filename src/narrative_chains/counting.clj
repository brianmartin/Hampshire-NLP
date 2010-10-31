(ns narrative-chains.counting
  (:use [clojure.contrib.combinatorics :only [combinations]]))

(defn find-entity
  "Finds an entity given the entity table for a specific
  sentence and the index of the word in that sentence."
  [entity-table word-idx]
  (first
  (filter #(not (nil? %))
    (for [e entity-table]
      (if (<= (first (e :span))
              word-idx
              (second (e :span)))
        (e :eid)
        nil)))))

(defn count-occurences-per-sentence
  "Adds sentence id's to the given dependency parses,
  along with resolving the enitities given the entity table
  of the document."
  [entity-table parses sid]
  (map #(assoc % :sid sid)
    (for [dep parses]
      (if (or (= (dep :dep) "agent")
              (= (dep :dep) "iobj")
              (= (dep :dep) "nsubj")
              (= (dep :dep) "dobj"))
        (assoc dep :eid (find-entity (filter #(= (:sid %) sid) entity-table)
                                     (dep :v2-i)))
        dep))))

(defn count-occurences
  "Given the document-wide entity-table and parses,
  Returns the dep-parses with resolved entities."
  [entity-table parses]
  (apply concat
    (doall
      (map #(count-occurences-per-sentence entity-table %1 (inc %2))
           parses (range)))))

(defn inc-count-map
  [c1 c2 m]
  (let [same-eid
          (fn [in] (if (= (:eid c1) (:eid c2))
                     (assoc
                       (assoc in :eid (conj (in :eid) (:eid c1)))
                       :cnt (inc (:cnt in)))
                     in))
        naive-cnt
          (fn [in] (assoc in :naive-cnt (inc (:naive-cnt in))))]
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
                   s (try #{(:v1 c1) (:v1 c2)}
                       (catch java.lang.IllegalArgumentException _
                              #{(:v1 c1)}))
                   m (if (nil? (count-map s)) {:cnt 0 :eid '() :naive-cnt 0} (count-map s))]
               (assoc count-map s (inc-count-map c1 c2 m)))
             (rest combos))
      count-map)))

(defn merge-two-values
  [v1 v2]
  (-> v1
    (assoc :cnt (+ (:cnt v1) (:cnt v2)))
    (assoc :eid (concat (:eid v1) (:eid v2)))
    (assoc :naive-cnt (+ (:naive-cnt v1) (:naive-cnt v2)))))

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
                   (let [m1-val (m1 key-in-?)
                         m2-val (m2 key-in-?)]
                     (println m1-val m2-val (merge-two-values m1-val m2-val))
                     (assoc m3 key-in-? (merge-two-values m1-val m2-val))))))
        m3))))

(defn merge-count-map-vector
  [count-map-vector]
  (reduce merge-two-count-maps count-map-vector))

(defn make-count-map2
  "Makes a map of counts of verbs sharing coreferring
  arguments where the keys are hash-sets of the two verbs
  (or one verb if two of the same verb co-occur)."
  [parses]
  (loop [count-map {}
         combos (combinations (filter :eid parses) 2)]
    (if (seq combos)
      (recur (let [combo (first combos)
                   [c1 c2] combo]
               (if (= (:eid c1) (:eid c2))
                 (let [s (try #{(:v1 c1) (:v1 c2) }
                           (catch java.lang.IllegalArgumentException _
                              #{(:v1 c1)}))
                       cell (count-map s)]
                    (if (nil? cell)
                      (assoc count-map s 1)
                      (assoc count-map s (inc cell))))
                 count-map))
             (rest combos))
      count-map)))
