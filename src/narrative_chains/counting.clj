(ns narrative-chains.counting)

(defn find-entity
  [entity-table word-idx]
  (first
  (filter #(not (nil? %))
    (for [e entity-table]
      (if (<= (first (e :span))
              word-idx
              (second (e :span)))
        (e :eid))))))

(defn count-occurences-per-sentence
  [entity-table parses sid]
  (map #(assoc % :sid sid)
    (for [dep parses]
      (if (or (= (dep :dep) "nsubj")
              (= (dep :dep) "nobj"))
        (assoc dep :eid (find-entity entity-table (dep :v2-i)))
        dep))))

(defn count-occurences
  [entity-table parses]
  (doall (map #(count-occurences-per-sentence entity-table %1 (inc %2)) parses (range))))
