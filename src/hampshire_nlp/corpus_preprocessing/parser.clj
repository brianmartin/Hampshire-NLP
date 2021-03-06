(ns hampshire-nlp.corpus-preprocessing.parser
  (:require [clojure.contrib.duck-streams :as d :only reader]
            [clojure.contrib.string :as string :only split-lines])
  (:import  [java.io PrintWriter StringWriter StringReader]
            [edu.stanford.nlp.trees TreePrint]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Stanford parsing

(defn parse->string
  "Converts a parse to String"
  [p print-type]
  (let [sw (StringWriter.)]
    (. (TreePrint. print-type) printTree p (PrintWriter. sw))
    (. sw toString)))

(defn parses->treebank-strings
  "Converts parses to treebank strings using Penn style"
  [parses]
  (map #(parse->string % "penn") parses))

(defn parses->dep-strings
  "Converts parses to treebank strings using collapsed type dependency style"
  [parses]
  (map #(parse->string % "typedDependenciesCollapsed") parses))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Pre-parser processing utilities

;; Plaintext:

(defn document->sentences
  "Extracts sentences from a document.  The document must be a StringReader or a File."
  [document dp]
  (with-open [rdr (d/reader document)]
    (let [sentences (. dp getSentencesFromText rdr)]
      (doall (for [s sentences] s)))))

(defn sentences->parses
  "Given sentences and a LexicalizedParser, returns best parses."
  [sentences lp]
  (for [s sentences]
    (do (. lp (parse s)) (. lp getBestParse))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Conversions to Clojure data structures:

(defn dep-string->clj
  "converts dep-string parses to clojure data structure (to be serialized)."
  [dep-string]
  (let [p (apply str (filter #(not= \' %) dep-string))]
  (try
    {:dep  (second (re-find #"^(.*)\(" p))
     :v1   (second (re-find #"\((.*)-(\d+)," p))
     :v1-i (Integer. (second (re-find #"-(\d+)," p)))
     :v2   (second (re-find #", (.*)-(\d+)\)" p))
     :v2-i (Integer. (second (re-find #"-(\d+)\)" p)))}
    (catch java.lang.NumberFormatException e (println "dep-strings: " e p)))))

(defn sentence-dep-strings->clj
  [sentence-parse]
  (doall (map dep-string->clj (string/split-lines sentence-parse))))

(defn document-dep-strings->clj
  "converts dep-string parses to clojure data structure (to be serialized)."
  [whole-document-parses]
  (doall (map sentence-dep-strings->clj whole-document-parses)))
