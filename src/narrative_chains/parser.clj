(ns narrative-chains.parser
  (:require [clojure.contrib.duck-streams :as d :only reader]
            [clojure.contrib.string :as string :only split-lines]
            [pl.danieljanus.tagsoup :as tagsoup])
  (:import  [java.io PrintWriter StringWriter StringReader]
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

(defn text-from-sgml
  [sgml]
  (let [id-and-type (second sgml) ;{:id "AFP_ENG_19940512.0004", :type "story"}
        text-vec (first
                   (filter #(not (nil? %))
                     (for [e sgml]
                       (if (vector? e)
                         (if (= :TEXT (first e)) e)))))
        text (. (apply str (for [p (drop 2 text-vec)] (nth p 2)))
                (replace \newline \space))]
    (assoc id-and-type :text text)))

(defn gigaword-file-to-documents
  "Given gigaword file path, gives vector of documents as maps with :type, :id, and :text."
  [file]
  (let [sgml-all (tagsoup/parse file)]
    (map text-from-sgml (drop 3 sgml-all))))

(defn document-to-parses
  [document charset lp dp]
  (file-to-parses (StringReader. (:text document)) charset lp dp))

(defn dep-string-to-clj
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

(defn sentence-dep-strings-to-clj
  [sentence-parse]
  (doall (map dep-string-to-clj (string/split-lines sentence-parse))))

(defn document-dep-strings-to-clj
  "converts dep-string parses to clojure data structure (to be serialized)."
  [whole-document-parses]
  (doall (map sentence-dep-strings-to-clj whole-document-parses)))

(defn entity-table-to-clj
  [entity-table]
  (if (nil? entity-table)
    nil
    (let [entries (string/split-lines entity-table)]
      (doall
        (map
          #(try
              (let [indices (re-find #"(\d+) (\d+) (\d+) (\d+) (.*)" %)]
                {:sid (Integer. (nth indices 1))
                 :eid (Integer. (nth indices 2))
                 :span [(Integer. (nth indices 3)) (Integer. (nth indices 4))]
                 :phrase (nth indices 5)})
             (catch java.lang.NumberFormatException e (println e %)))
          entries)))))

(defn parse-to-string
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
  "Converts parses to treebank strings using collapsed type dependency style"
  [parses]
  (map #(parse-to-string % "typedDependenciesCollapsed") parses))
