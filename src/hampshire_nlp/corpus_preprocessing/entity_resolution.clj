(ns hampshire-nlp.corpus-preprocessing.entity-resolution)

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
