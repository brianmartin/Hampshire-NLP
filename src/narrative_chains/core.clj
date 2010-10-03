(ns narrative-chains.core
  (:require [clojure.contrib.duck-streams :as d :only file-str]
            [clojure.contrib.command-line :as cl]
            [narrative-chains.parser :as p]
            [narrative-chains.coref :as c])
  (:import [edu.stanford.nlp.process DocumentPreprocessor]
           [edu.stanford.nlp.parser.lexparser LexicalizedParser]))

(defn -main [& args]
  (cl/with-command-line args "Parse and Coref"
    [[input-dir i "Folder containing input files." "~/test-data"]
     [output-dir o "Destination folder for output files." "~/output-data"]
     [charset c "Charset of input." "utf-8"]
     [grammar g "Grammar file for Stanford Parser." "data/englishPCFG.ser.gz"]
     [wordnet w "Wordnet dir (for JWNL)" "data/wordnet"]
     etc]

  (System/setProperty "WNSEARCHDIR" wordnet)

  (let [lp (LexicalizedParser. grammar)
        dp (DocumentPreprocessor.)
        idir (d/file-str input-dir)
        files (. idir listFiles)
        file-cnt (count files)
        odir (d/file-str output-dir)
        documents
          (map #(do (println "Initial parse of file: " (inc %2) "/" file-cnt)
                    (p/file-to-parses % charset lp dp)) files (range file-cnt))
        stanford-dep-parses 
          (map #(do (println "Stanford parse of file: " (inc %2) "/" file-cnt)
                    (p/parses-to-dep-strings %)) documents (range file-cnt))
        stringed-parses
          (map #(do (println "Stringing parse of file: " (inc %2) "/" file-cnt)
                    (p/parses-to-treebank-strings %)) documents (range file-cnt))
        entity-tables
          (map #(do (println "Entity table for file: " (inc %2) "/" file-cnt)
                    (c/process-parses %)) stringed-parses (range file-cnt))]
    (println entity-tables))))
