(ns narrative-chains.core
  (:require [clojure.contrib.duck-streams :as d]
            [clojure.contrib.command-line :as cl]
            [narrative-chains.parser :as p]
            [narrative-chains.coref :as c])
  (:import [java.io File]
           [edu.stanford.nlp.process DocumentPreprocessor]
           [edu.stanford.nlp.parser.lexparser LexicalizedParser])
  (:gen-class))

(defn -main [& args]
  "Main method of 'narrative-chains'.  Parses files in an input directory,
  performs coref, and writes results to the output directory."
  (cl/with-command-line args "Parse and Coref"
    [[input-dir i "Folder containing input files." "~/test-data"]
     [output-dir o "Destination folder for output files." "~/output-data"]
     [charset c "Charset of input." "utf-8"]
     [grammar g "Grammar file for Stanford Parser." "data/englishPCFG.ser.gz"]
     [coref c "Coref data directory for OpenNLP." "data/coref"]
     [wordnet w "Wordnet dir (for JWNL)" "data/wordnet"]
     etc]

  (intern 'narrative-chains.coref 'resource coref)
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
    (dotimes [i (count files)]
      (let [f-name (.getName (nth files i))
            parent (d/file-str (str output-dir "/" f-name))]
        (.mkdir parent)
        (dotimes [j (count (nth stanford-dep-parses i))]
          (d/write-lines (File. parent (str "sdep." j))
                         (-> stanford-dep-parses (nth i) (nth j) (list))))
        (d/write-lines (File. parent "enity-table") (list (nth entity-tables i))))))))
