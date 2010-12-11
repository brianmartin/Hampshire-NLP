(ns hampshire-nlp.corpus-preprocessing.core
  (:use [hampshire-nlp.rabbitmq]
        [clojure.contrib.command-line]
        [clojure.contrib.duck-streams :only [file-str]])
  (:require [hampshire-nlp.corpus-preprocessing.parser :as p]
            [hampshire-nlp.corpus-preprocessing.coref :as c]
            [hampshire-nlp.corpus-preprocessing.xml :as x])
  (:import [java.io File StringReader]
           [edu.stanford.nlp.process DocumentPreprocessor]
           [edu.stanford.nlp.parser.lexparser LexicalizedParser]))

(defn process
  "Does parsing and entity resolution on the file given and writes output to xml
  in the output-dir."
  [file lp dp output-dir]
  (doseq [document (x/gigaword->documents file)]
    (if (= "story" (:type document))
      (let [sentences (p/document->sentences (StringReader. (:text document)) dp)
            parses (p/sentences->parses sentences lp)
            dep-parses (p/document-dep-strings->clj (p/parses->dep-strings parses))
            entity-table (p/entity-table->clj (c/process-parses (p/parses->treebank-strings parses)))]
        (x/record-as-xml (File. output-dir (str (.getName file) ".xml"))
                         (:id document) (:type document) sentences dep-parses entity-table)))))

(defn process-off-queue
  "Processes one file of of the queue."
  [output-dir grammar]
  (let [msg (get-msg)]
    (if msg
      (let [lp (LexicalizedParser. grammar)
            _ (. lp setMaxLength 80)
            dp (DocumentPreprocessor.)]
        (println msg)
        (process (File. msg) lp dp output-dir)
        (println "done " msg)))))

(defn start-worker
  "Process files from the queue (if none, waits 5 seconds, recheck,... forever)."
  [output-dir grammar]
  (while true
    (process-off-queue output-dir grammar)
    (Thread/sleep 5000)))

(defn run
  "Parses files in an input directory, performs coref, and writes results
  to the output directory in xml convenient for further processing."
  [{input-dir :input-dir
    output-dir :output-dir
    grammar :grammar
    coref :coref
    wordnet :wordnet
    job-dist? :job-dist?
    host :host
    user :user
    pass :pass
    port :port
    debug? :debug?}]

  (intern 'hampshire-nlp.corpus-preprocessing.coref 'resource coref)
  (System/setProperty "WNSEARCHDIR" wordnet)

  (init-connection {:username user :password pass
                    :virtual-host "/" :host host :port port})

  (if job-dist?
    (dispatch-all-file-paths input-dir)
    (start-worker (file-str output-dir) grammar))

  (if debug?
    (process-off-queue (file-str output-dir) grammar)))
