(ns narrative-chains.core
  (:use [rabbitcj.client])
  (:require [clojure.contrib.duck-streams :as d]
            [clojure.contrib.command-line :as cl]
            [narrative-chains.parser :as p]
            [narrative-chains.coref :as c])
  (:import [java.io File]
           [edu.stanford.nlp.process DocumentPreprocessor]
           [edu.stanford.nlp.parser.lexparser LexicalizedParser])
  (:gen-class))

(def conn nil)
(def chan nil)

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

(defn run-one
  [file lp dp charset output-dir]
  (let [parent (File. output-dir (.getName file))
        document (p/file-to-parses file charset lp dp)
        stanford-dep-parse (p/parses-to-dep-strings document)
        stringed-parse (p/parses-to-treebank-strings document)
        entity-table (c/process-parses stringed-parse)]
    (.mkdir parent)
    (record-parses parent (vec stanford-dep-parse))
    (record-entity-table parent entity-table)))

(defn get-msg [] (String. (.. chan (basicGet "nlp" true) (getBody))))

(defn run
  "Run on files from the queue."
  [output-dir grammar charset]
  (let [lp (LexicalizedParser. grammar)
        dp (DocumentPreprocessor.)]
    (loop [msg (get-msg)]
      (if (not (nil? msg))
        (do
          (run-one (File. msg) lp dp charset output-dir)
          (println "done " msg)
          (recur (get-msg)))))))

(defn dispatch
  "Put all file paths in the input directory into the queue."
  [input-dir]
  (let [files (.listFiles (d/file-str input-dir))]
    (declare-exchange chan "nlp" fanout-exchange false false nil)
    (declare-queue chan "nlp" false false true nil)
    (bind-queue chan "nlp" "nlp" "")
    (doall (map #(publish chan "nlp" "" (.getCanonicalPath %)) files))))

(defn -main [& args]
  "Main method of 'narrative-chains'.  Parses files in an input directory,
  performs coref, and writes results to the output directory."
  (cl/with-command-line args "Parse and Coref"
    [[input-dir i "Folder containing input files." "~/wrk/nlp/test-data"]
     [output-dir o "Destination folder for output files." "~/wrk/nlp/output-data"]
     [charset c "Charset of input." "utf-8"]
     [grammar g "Grammar file for Stanford Parser." "data/englishPCFG.ser.gz"]
     [coref c "Coref data directory for OpenNLP." "data/coref"]
     [wordnet w "Wordnet dir (for JWNL)" "data/wordnet"]
     [job-dist? j? "Distributor of jobs?"]
     etc]

  (intern 'narrative-chains.coref 'resource coref)
  (System/setProperty "WNSEARCHDIR" wordnet)

  (def conn (connect {:username "guest" :password "guest" 
                      :virtual-host "/" :host "127.0.0.1" :port 5672}))
  (def chan (create-channel conn))

  (if job-dist?
    (dispatch input-dir)
    (run (d/file-str output-dir) grammar charset))))
