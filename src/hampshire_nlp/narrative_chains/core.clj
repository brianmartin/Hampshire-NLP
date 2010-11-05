(ns hampshire-nlp.narrative-chains.core
  (:use [rabbitcj.client]
        [clojure.pprint :only [pprint]])
  (:require [clojure.contrib.duck-streams :only [file-str append-writer] :as d]
            [clojure.contrib.command-line :as cl]
            [narrative-chains.parser :as p]
            [narrative-chains.coref :as c]
            [narrative-chains.counting :as counting])
  (:import [java.io File]
           [edu.stanford.nlp.process DocumentPreprocessor]
           [edu.stanford.nlp.parser.lexparser LexicalizedParser])
  (:gen-class))

(def conn nil)
(def chan nil)
(def parses [])
(def entity-table [])
(def counts [])

(defn record
  [parent file-name data]
  (with-open [writer (d/append-writer (File. parent file-name))]
    (. writer (write (prn-str data)))))

(defn run-one
  [file lp dp charset output-dir]
  (let [parent (File. output-dir (.getName file))
        documents (p/gigaword-file-to-documents file)]
    (.mkdir parent)
    (doseq [document documents]
      (let [parses (p/document-to-parses document charset lp dp)
            dep-parse-strings (p/parses-to-dep-strings parses)
            dep-parses (p/document-dep-strings-to-clj dep-parse-strings)
            stringed-parse (p/parses-to-treebank-strings parses)
            ent-table (p/entity-table-to-clj (c/process-parses stringed-parse))
            dep-parses-with-entities (counting/count-occurences ent-table dep-parses)
            count-map (counting/make-count-map dep-parses-with-entities)]
      (record parent "dep-parses" dep-parses-with-entities)
      (record parent "entity-table" ent-table)
      (record parent "count-maps" count-map)
      (def parses (conj parses dep-parses-with-entities))
      (def entity-table (conj entity-table ent-table))
      (def counts (conj counts count-map))))
    (let [merged-count-map (counting/merge-count-map-vector counts)]
      (record parent "merged-count-map" counts)
      (def merged-counts merged-count-map))))

(defn get-msg [] (String. (.. chan (basicGet "nlp" true) (getBody))))

(defn debug-run-one
  [output-dir grammar charset]
  (let [lp (LexicalizedParser. grammar)
        dp (DocumentPreprocessor.)
        msg (get-msg)]
    (if (not (nil? msg))
      (do
        (run-one (File. msg) lp dp charset output-dir)
        (println "done " msg)))))

(defn run
  "Run on files from the queue."
  [output-dir grammar charset]
  (let [lp (LexicalizedParser. grammar)
        dp (DocumentPreprocessor.)]
    (loop [msg (get-msg)]
      (if (not (nil? msg))
        (do
          (run-one (File. msg) lp dp charset output-dir)
          (println "done " msg))
        (Thread/sleep 10000))
      (recur (try (get-msg) (catch Exception _ nil))))))

(defn dispatch
  "Put all file paths in the input directory into the queue."
  [input-dir]
  (let [files (.listFiles (d/file-str input-dir))]
    (declare-exchange chan "nlp" fanout-exchange false false nil)
    (declare-queue chan "nlp" false false true nil)
    (bind-queue chan "nlp" "nlp" "")
    (doall (map #(publish chan "nlp" "" (.getCanonicalPath %)) files))))

(defn merge-all
  "Merges all count-maps in the output-directory.  For example:
  ouput-dir/{a,b,c}/merged-count-map -> output-dir/mega-merged-count-map"
  [output-dir]
  (let [mega
         (-> (filter #(not (nil? %))
               (doall (for [f (.listFiles output-dir)]
                 (when (.isDirectory f)
                   (load-string (slurp (str (.getCanonicalPath f) "/merged-count-map")))))))
           (first)
           (counting/merge-count-map-vector))]
    (record output-dir "mega-merged-count-map" mega)
    (def mega-merged mega)))

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
     [job-dist? j? "Distributor of jobs?"]
     [debug? d? "Run through only one file for debugging."]
     etc]

  (intern 'narrative-chains.coref 'resource coref)
  (System/setProperty "WNSEARCHDIR" wordnet)

  (def conn (connect {:username "guest" :password "guest" 
                      :virtual-host "/" :host "127.0.0.1" :port 5672}))
  (def chan (create-channel conn))

  (if job-dist?
    (dispatch input-dir)
    (if debug?
      (debug-run-one (d/file-str output-dir) grammar charset)
      (run (d/file-str output-dir) grammar charset)))))
