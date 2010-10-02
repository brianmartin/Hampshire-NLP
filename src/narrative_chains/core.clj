(ns narrative-chains.core
  (:use [clojure.contrib.duck-streams :as d]
        [clojure.contrib.command-line])
  (:import [java.io PrintWriter StringWriter]
           [edu.stanford.nlp.process DocumentPreprocessor]
           [edu.stanford.nlp.parser.lexparser LexicalizedParser]
           [edu.stanford.nlp.trees TreePrint]
           [edu.stanford.nlp.ling Word HasWord]
           [opennlp.tools.coref DefaultLinker DiscourseEntity Linker LinkerMode]
           [opennlp.tools.coref.mention DefaultParse Mention MentionContext PTBMentionFinder]
           [opennlp.tools.parser Parse]
           [opennlp.tools.parser.chunking Parser]
           [opennlp.tools.util Span]
           [opennlp.tools.lang.english TreebankLinker]))

(defn file-to-parses
  [file charset lp dp]
  (with-open [rdr (reader file)]
    (let [sentences (. dp getSentencesFromText rdr)]
      (doall
        (for [s sentences]
          (do (. lp (parse s)) (. lp getBestParse)))))))

(defn parse-to-string
  [p print-type]
  (let [sw (StringWriter.)]
    (. (TreePrint. print-type) printTree p (PrintWriter. sw))
    (. sw toString)))

(defn parses-to-treebank-strings
  [parses]
  (map #(parse-to-string % "penn") parses))

(defn parses-to-dep-strings
  [parses]
  (map #(parse-to-string % "typedDependenciesCollapsed") parses))

(defn -main [& args]
  (with-command-line args "Parse and Coref"
    [[input-dir i "Folder containing input files." "~/test-data"]
     [output-dir o "Destination folder for output files." "~/output-data"]
     [charset c "Charset of input." "utf-8"]
     [grammar g "Grammar file for Stanford Parser." "data/englishPCFG.ser.gz"]
     etc]

  (let [lp (LexicalizedParser. grammar)
        dp (DocumentPreprocessor.)
        idir (d/file-str input-dir)
        files (. idir listFiles)
        file-cnt (count files)
        odir (d/file-str output-dir)
        parses
          (loop [data [] fs files cnt 1]
            (if (seq fs)
              (do 
                (println "Initial parse of file: " cnt " / " file-cnt)
                (recur (conj data (file-to-parses (first fs) charset lp dp))
                       (rest fs)
                       (inc cnt)))
              data))
        stanford-dep-parses 
          (loop [data [] ps parses cnt 1]
            (if (seq ps)
              (do
                (println "Stanford parse of file: " cnt " / " file-cnt)
                (recur (conj data (parses-to-dep-strings (first ps)))
                       (rest ps)
                       (inc cnt)))
              data))
        stringed-parses
          (loop [data [] ps parses cnt 1]
            (if (seq ps)
              (do
                (println "Stringing parse of file: " cnt " / " file-cnt)
                (recur (conj data (parses-to-treebank-strings (first ps)))
                       (rest ps)
                       (inc cnt)))
              data))
        entity-tables
          (loop [data [] ps parses cnt 1]
            (if (seq ps)
              (do  
                (println "Entity table for file: " cnt " / " file-cnt)
                (recur (conj data (coref/process-parse (first ps)))
                       (rest ps)
                       (inc cnt)))))]
    )))
