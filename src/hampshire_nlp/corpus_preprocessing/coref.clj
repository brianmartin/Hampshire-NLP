(ns hampshire-nlp.corpus-preprocessing.coref
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

(defn word-span
  "Given text and character span, returns word span."
  [text span]
  (let [begin-word (word-index text (first span))
        end-word (+ begin-word (word-index (drop (first span) text) (- (second span) (first span))))]
    [begin-word end-word]))

(defn- show-entities
  "Returns a string of the entity-table given mentions, a linker, and the text.
  (silently drops exceptions, returning nil)"
  [all-extents linker sentences]
  (try
    (let [mentions (. all-extents toArray (make-array Mention (. all-extents size)))
          entities (. linker getEntities mentions)
          entity-table (atom [])]
      (dotimes [i (alength entities)]
        (let [e (aget entities i)
              iter (. e getMentions)]
          (while (. iter hasNext)
            (let [mc (. iter next)
                  sid (. mc getSentenceNumber)
                  phrase (. mc toText)
                  text (nth sentences (dec sid))
                  mc-span (. mc getSpan)
                  span (word-span text (. mc-span getStart) (. mc-span getEnd))]
              (reset! entity-table (conj @entity-table {:sid sid :eid i :phrase phrase :span span}))))))
      (@entity-table))
  (catch java.lang.Exception _ nil)))

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
