(ns hampshire-nlp.core
  (:require [hampshire-nlp.corpus-preprocessing.core :as corp])
            ;[hampshire-nlp.narrative-chains.core :as narr]
            ;[hampshire-nlp.relation-extraction.core :as rela])
  (:use [clojure.contrib.command-line])
  (:gen-class))

(defn -main [& args]
  (with-command-line args "Hampshire NLP Package"
    [[corpus-preprocessing? p? "Perform pre-processing on the corpus."]
     [narrative-chains? n? "Run narrative-chains on a pre-processed corpus."]
     [relation-extraction? e? "Run the relation-extractor on a pre-processed corpus."]
     [input-dir i "Folder containing input files." "~/test-data"]
     [output-dir o "Destination folder for output files." "~/output-data"]
     [charset c "Charset of input." "utf-8"]
     [grammar g "Grammar file for Stanford Parser." "data/englishPCFG.ser.gz"]
     [coref c "Coref data directory for OpenNLP." "data/coref"]
     [wordnet w "Wordnet dir (for JWNL)" "data/wordnet"]
     [job-dist? j? "Distributor of jobs?"]
     [debug? d? "Run through only one file for debugging."]
     etc]

    (let [arg-map {:input-dir input-dir :output-dir output-dir :charset charset :grammar grammar
                   :coref coref :wordnet wordnet :job-dist? job-dist? :debug? debug?}]
      (cond corpus-preprocessing? (corp/run arg-map)
            ;narrative-chains?    (narr/run arg-map)
            ;relation-extraction? (rela/run arg-map)
            :else (System/exit 0)))))
