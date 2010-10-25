(ns narrative-chains.counting
  (:require [clojure.contrib.combinatorics :only combinations :as c]))

(defn find-entity
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
  [entity-table parses sid]
  (map #(assoc % :sid sid)
    (for [dep parses]
      (if (or (= (dep :dep) "nsubj")
              (= (dep :dep) "nobj"))
        (assoc dep :eid (find-entity (filter #(= (:sid %) sid) entity-table)
                                     (dep :v2-i)))
        dep))))

(defn count-occurences
  [entity-table parses]
  (apply concat
    (doall
      (map #(count-occurences-per-sentence entity-table %1 (inc %2))
           parses (range)))))

(defn make-count-map
  [parses]
  (loop [count-map {}
         combos (c/combinations (filter :eid parses) 2)]
    (println count-map (first combos))
    (if (seq combos)
      (recur (let [combo (first combos)
                   v1 (:v1 (first combo))
                   v2 (:v1 (second combo))]
               (if (= v1 v2)
                 (let [s (try #{v1 v2}
                           (catch java.lang.IllegalArgumentException _
                              #{v1}))
                       cell (count-map s)]
                    (if (nil? cell)
                      (assoc count-map s 1)
                      (assoc count-map s (inc cell))))
                 count-map))
             (rest combos))
      count-map)))
