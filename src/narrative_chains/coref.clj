(ns narrative-chains.coref
  (:import [java.util ArrayList Arrays]
           [opennlp.tools.coref DefaultLinker DiscourseEntity Linker LinkerMode]
           [opennlp.tools.coref.mention DefaultParse Mention MentionContext PTBMentionFinder]
           [opennlp.tools.parser Parse]
           [opennlp.tools.lang.english TreebankLinker]))

(def resource nil)

(defn word-index
  [text idx]
  (loop [i 0 s 1]
    (if (< i idx)
      (if (= \space (nth text i))
        (recur (inc i) (inc s))
        (recur (inc i) s))
      s)))

(defn table-format
  [sid phrase span text eid]
  (apply str (interpose \space (list
    sid eid (word-index text (. span getStart)) (word-index text (. span getEnd)) phrase))))

(defn show-entities
  [all-extents linker sent-text]
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
                                          (. mc getSpan) (nth sent-text (dec s-num)) i) "\n"))))))
    (. entity-table toString)))

(defn process-parses
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
                 (conj sent-text (first ps))))
        (show-entities all-extents linker sent-text)))))
