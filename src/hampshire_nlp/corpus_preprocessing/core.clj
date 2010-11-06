(ns hampshire-nlp.corpus-preprocessing.core
  (:use [hampshire-nlp.dispatch]
        [clojure.contrib.command-line]
        [clojure.contrib.duck-streams :only [file-str]])
  (:require [hampshire-nlp.corpus-preprocessing.parser :as p]
            [hampshire-nlp.corpus-preprocessing.coref :as c]
            [hampshire-nlp.corpus-preprocessing.xml :as x])
  (:import [java.io File StringReader]
           [edu.stanford.nlp.process DocumentPreprocessor]
           [edu.stanford.nlp.parser.lexparser LexicalizedParser])
  (:gen-class))

(defn process
  "Does parsing and entity resolution on the file given and writes output to xml
  in the output-dir."
  [file lp dp charset output-dir]
  (let [parent (File. output-dir (.getName file))
        documents (p/gigaword->documents file)]
    (.mkdir parent)
    (doseq [document documents]
      (let [sentences (p/document->sentences (StringReader. (:text document)) dp)
            parses (p/sentences->parses document lp)
            dep-parses (p/document-dep-strings->clj (p/parses->dep-strings parses))
            entity-table (p/entity-table->clj (c/process-parses (p/parses->treebank-strings parses)))]
      (x/record-as-xml parent (str (.getName file) ".xml") sentences dep-parses entity-table)))))

(defn process-one
  "Processes one file of of the queue."
  [output-dir grammar charset]
  (let [lp (LexicalizedParser. grammar)
        dp (DocumentPreprocessor.)
        msg (get-msg)]
    (if (not (nil? msg))
      (process (File. msg) lp dp charset output-dir))))

(defn start-worker
  "Process files from the queue (if none, waits 10 seconds)."
  [output-dir grammar charset]
  (let [lp (LexicalizedParser. grammar)
        dp (DocumentPreprocessor.)]
    (loop [msg (get-msg)]
      (if (not (nil? msg))
        (do
          (process (File. msg) lp dp charset output-dir)
          (println "done " msg))
        (Thread/sleep 10000))
      (recur (try (get-msg) (catch Exception _ nil))))))

(defn -main [& args]
  "Parses files in an input directory, performs coref, and writes results
  to the output directory in xml convenient for further processing."
  (with-command-line args "Parse and Coref"
    [[input-dir i "Folder containing input files." "~/test-data"]
     [output-dir o "Destination folder for output files." "~/output-data"]
     [charset c "Charset of input." "utf-8"]
     [grammar g "Grammar file for Stanford Parser." "data/englishPCFG.ser.gz"]
     [coref c "Coref data directory for OpenNLP." "data/coref"]
     [wordnet w "Wordnet dir (for JWNL)" "data/wordnet"]
     [job-dist? j? "Distributor of jobs?"]
     [debug? d? "Run through only one file for debugging."]
     etc]

  (intern 'hampshire-nlp.corpus-preprocessing.coref 'resource coref)
  (System/setProperty "WNSEARCHDIR" wordnet)

  (init-connection {:username "guest" :password "guest"
                    :virtual-host "/" :host "127.0.0.1" :port 5672})

  (if job-dist?
    (dispatch-all-file-paths input-dir)
    (if debug?
      (process-one (file-str output-dir) grammar charset)
      (start-worker (file-str output-dir) grammar charset)))))
