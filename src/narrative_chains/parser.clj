(ns narrative-chains.parser
  (:use    [clojure.contrib.duck-streams])
  (:import [java.io PrintWriter StringWriter]
           [edu.stanford.nlp.process DocumentPreprocessor]
           [edu.stanford.nlp.parser.lexparser LexicalizedParser]
           [edu.stanford.nlp.trees TreePrint]))

(defn file-to-parses
  [file charset lp dp]
  (with-open [rdr (reader file)]
    (let [sentences (. dp getSentencesFromText rdr)]
      (doall
        (for [s sentences]
          (do (. lp (parse s)) (. lp getBestParse)))))))

(defn parse-to-string
  [p print-type]
  (let [sw (StringWriter.)]
    (. (TreePrint. print-type) printTree p (PrintWriter. sw))
    (. sw toString)))

(defn parses-to-treebank-strings
  [parses]
  (map #(parse-to-string % "penn") parses))

(defn parses-to-dep-strings
  [parses]
  (map #(parse-to-string % "typedDependenciesCollapsed") parses))
