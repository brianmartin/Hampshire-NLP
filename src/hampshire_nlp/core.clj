(ns hampshire-nlp.core
  (:require [hampshire-nlp.corpus-preprocessing.core :as corp]
            [hampshire-nlp.narrative-chains.core :as narr])
            ;[hampshire-nlp.relation-extraction.core :as rela])
  (:use [clojure.contrib.command-line]
        [clojure.contrib.duck-streams :only [with-out-writer file-str]])
  (:gen-class))

(defn -main [& args]
  (with-command-line args "Hampshire NLP Package"
    [[corpus-preprocessing? p? "Perform pre-processing on the corpus."]
     [narrative-chains? n? "Run narrative-chains on a pre-processed corpus."]
     [relation-extraction? e? "Run the relation-extractor on a pre-processed corpus."]
     [log-dir l "If specified, output will redirect to this dir (+ hostname and current time)" nil]
     [raw-corpus-dir i "Folder containing raw input files." "~/wrk/nlp/raw-corpus"]
     [processed-corpus-dir z "Folder containing processed input files." "~/wrk/nlp/preprocessed-corpus"]
     [narr-chains-output-dir o "Destination folder for narrative chains output." "~/narrative-chains-output"]
     [relation-ext-output-dir q "Destination folder for relation extractor output." "~/relation-extraction-output"]
     [grammar g "Grammar file for Stanford Parser." "data/englishPCFG.ser.gz"]
     [coref c "Coref data directory for OpenNLP." "data/coref"]
     [wordnet w "Wordnet dir (for JWNL)" "data/wordnet"]
     [job-dist? j? "Distributor of jobs?"]
     [host h "RabbitMQ hostname." "127.0.0.1"]
     [user u "RabbitMQ username." "guest"]
     [pass w "RabbitMQ password." "guest"]
     [debug? d? "Run through only one file for debugging."]
     etc]

    (intern 'clojure.contrib.prxml '*prxml-indent* 2)

    (let [arg-map {:input-dir (if corpus-preprocessing? raw-corpus-dir processed-corpus-dir)
                   :output-dir (cond corpus-preprocessing? processed-corpus-dir
                                     narrative-chains? narr-chains-output-dir
                                     relation-extraction? relation-ext-output-dir)
                   :grammar grammar :coref coref :wordnet wordnet :job-dist? job-dist? :debug? debug?
                   :host host :user user :pass pass :port 5672}]
      (if log-dir
        (with-out-writer (file-str (str log-dir "/" (.. java.net.InetAddress getLocalHost getHostName) "_"
                                                     (System/currentTimeMillis)))
          (cond corpus-preprocessing? (corp/run arg-map)
                narrative-chains?     (narr/run arg-map)
                ;relation-extraction? (rela/run arg-map)
                :else nil))
        (cond corpus-preprocessing? (corp/run arg-map)
              narrative-chains?     (narr/run arg-map)
              ;relation-extraction? (rela/run arg-map)
              :else nil))))
    (System/exit 0))
