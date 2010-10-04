(ns narrative-chains.core
  (:require [clojure.contrib.duck-streams :as d]
            [clojure.contrib.command-line :as cl]
            [narrative-chains.parser :as p]
            [narrative-chains.coref :as c])
  (:import [java.io File]
           [edu.stanford.nlp.process DocumentPreprocessor]
           [edu.stanford.nlp.parser.lexparser LexicalizedParser])
  (:gen-class))

(def complete (agent 0))

(defn record-parses
  "Write sentence parses to files."
  [parent parses]
  (dotimes [i (count parses)]
    (d/write-lines (File. parent (str "sdep." i))
      (list (nth parses i)))))

(defn record-entity-table
  "Write entity-table to file."
  [parent table]
  (d/write-lines (File. parent "entity-table") (list table)))

(defn run
  "Pipelining files through processing."
  [files lp dp charset output-dir-name output-dir batch-number]
  (let [documents (map #(p/file-to-parses % charset lp dp) files)
        stanford-dep-parses (map p/parses-to-dep-strings documents)
        stringed-parses (map p/parses-to-treebank-strings documents)
        entity-tables (map c/process-parses stringed-parses)]
    (dotimes [i (count files)]
      (let [parent (d/file-str (str output-dir-name "/" (.getName (nth files i))))]
        (.mkdir parent)
        (record-parses parent (nth stanford-dep-parses i))
        (record-entity-table parent (nth entity-tables i))
        (send complete inc)))))

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

  (let [output-dir-File (d/file-str output-dir)
        lp (LexicalizedParser. grammar)
        files (.listFiles (d/file-str input-dir))
        file-cnt (count files)]
    (add-watch complete :k #(println %4 "/" file-cnt))
    (doall (map #(run %1 lp (DocumentPreprocessor.) charset output-dir output-dir-File %2) 
                 (partition 3 files) (range)))))
  (System/exit 0))
