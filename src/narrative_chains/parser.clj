(ns narrative-chains.parser
  (:require [clojure.contrib.duck-streams :as d :only reader])
  (:import  [java.io PrintWriter StringWriter]
            [edu.stanford.nlp.trees TreePrint]))

(defn file-to-parses
  "Given a file, charset, LexicalizedParser, and DocumentPreproccessor...
  Parses file contents."
  [file charset lp dp]
  (with-open [rdr (d/reader file)]
    (let [sentences (. dp getSentencesFromText rdr)]
      (doall
        (for [s sentences]
          (do (. lp (parse s)) (. lp getBestParse)))))))

(defn- parse-to-string
  "Converts a parse to String"
  [p print-type]
  (let [sw (StringWriter.)]
    (. (TreePrint. print-type) printTree p (PrintWriter. sw))
    (. sw toString)))

(defn parses-to-treebank-strings
  "Converts parses to treebank strings using Penn style"
  [parses]
  (map #(parse-to-string % "penn") parses))

(defn parses-to-dep-strings
  "Converts parses to treebank strings styled as collapsed typed dependencies."
  [parses]
  (map #(parse-to-string % "typedDependenciesCollapsed") parses))
