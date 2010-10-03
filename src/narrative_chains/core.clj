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
          (loop [data [] fs files cnt 1]
            (if (seq fs)
              (do 
                (println "Initial parse of file: " cnt " / " file-cnt)
                (recur (conj data (p/file-to-parses (first fs) charset lp dp))
                       (rest fs)
                       (inc cnt)))
              data))
        stanford-dep-parses 
          (loop [data [] ds documents cnt 1]
            (if (seq ds)
              (do
                (println "Stanford parse of file: " cnt " / " file-cnt)
                (recur (conj data (p/parses-to-dep-strings (first ds)))
                       (rest ds)
                       (inc cnt)))
              data))
        stringed-parses
          (loop [data [] ds documents cnt 1]
            (if (seq ds)
              (do
                (println "Stringing parse of file: " cnt " / " file-cnt)
                (recur (conj data (p/parses-to-treebank-strings (first ds)))
                       (rest ds)
                       (inc cnt)))
              data))
        entity-tables
          (loop [data [] ps stringed-parses cnt 1]
            (if (seq ps)
              (do  
                (println "Entity table for file: " cnt " / " file-cnt)
                (recur (conj data (c/process-parses (first ps)))
                       (rest ps)
                       (inc cnt)))
              data))]
    (println entity-tables))))
