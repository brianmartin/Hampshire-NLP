(ns narrative-chains.coref
  (:import [java.util ArrayList Arrays]
           [opennlp.tools.coref DefaultLinker DiscourseEntity Linker LinkerMode]
           [opennlp.tools.coref.mention DefaultParse Mention MentionContext PTBMentionFinder]
           [opennlp.tools.parser Parse]
           [opennlp.tools.lang.english TreebankLinker]))

(def resource nil)

(defn word-index
  "Given text and character index, returns word index."
  [text idx]
  (->> text (take idx)
            (filter #(= \space %))
            (count)
            (inc)))

(defn- table-format
  "Formats information for placing into the entity-table."
  [sid phrase span text eid]
  (apply str (interpose \space (list
    sid eid (word-index text (. span getStart)) (word-index text (. span getEnd)) phrase))))

(defn- show-entities
  "Returns a string of the entity-table given mentions, a linker, and the text."
  [all-extents linker sentences]
  (let [mentions (. all-extents toArray (make-array Mention (. all-extents size)))
        entities (. linker getEntities mentions)
        entity-table (StringBuilder.)]
    (dotimes [i (alength entities)]
      (let [e (aget entities i)
            iter (. e getMentions)]
        (while (. iter hasNext)
          (let [mc (. iter next)
                s-num (. mc getSentenceNumber)]
            (. entity-table append (str (table-format s-num (. mc toText)
                                          (. mc getSpan) (nth sentences (dec s-num)) i) "\n"))))))
    (. entity-table toString)))

(defn process-parses
  "Gets mentions from the parses and puts them into a string representation of an entity table."
  [parses]
  (let [linker (TreebankLinker. resource (. LinkerMode TEST))
        all-extents (ArrayList.)]
    (loop [ps parses sent-num 1 sent-text []]
      (if (seq ps)
        (let [p (. Parse parseParse (. (first ps) replaceAll "ROOT" "TOP"))
              extents (.. linker (getMentionFinder) (getMentions (DefaultParse. p sent-num)))] 
          (if (> (count (first  ps)))
            (dotimes [i (alength extents)]
              (if (not (. (aget extents i) getParse))
                (let [snp (Parse. (. p getText) (. (aget extents i) getSpan) "NML" 1.0 0)]
                  (. p insert snp)
                  (. (aget extents i) setParse (DefaultParse. snp sent-num))))))
          (. all-extents addAll (. Arrays asList extents))
          (recur (rest ps)
                 (inc sent-num)
                 (conj sent-text (. p getText))))
        (show-entities all-extents linker sent-text)))))
