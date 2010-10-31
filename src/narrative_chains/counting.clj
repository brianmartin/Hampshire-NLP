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

(defn make-count-map
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
                       m (count-map s)]
                    (if (nil? m)
                      (assoc count-map s {:cnt 1 :eid (list (:eid c1))})
                      (assoc count-map s (assoc (assoc m :eid (conj (m :eid) (:eid c1))) :cnt (inc (m :cnt))))))
                 count-map))
             (rest combos))
      count-map)))

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
