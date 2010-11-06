(ns hampshire-nlp.corpus-preprocessing.xml
  (:use [clojure.contrib.prxml]
        [clojure.contrib.duck-streams :only [append-writer]]
        [hampshire-nlp.corpus-preprocessing.entity-resolution])
  (:import [java.io File]))

(defn write
  [parent file-name data]
  (with-open [writer (append-writer (File. parent file-name))]
    (. writer (write (prxml data))))) ;not sure this will work

(defn record-as-xml
  [parent file-name sentences dep-parses entity-table]
  nil)
