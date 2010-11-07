(ns hampshire-nlp.corpus-preprocessing.xml
  (:use [clojure.contrib.prxml]
        [clojure.contrib.duck-streams :only [append-writer]]
        [hampshire-nlp.corpus-preprocessing.entity-resolution])
  (:import [java.io File]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; XML output

(defmacro with-out-append-writer
  "Opens a writer on f, binds it to *out*, and evalutes body.
  Anything printed within body will be appended to f."
  [f & body]
  `(with-open [stream# (append-writer ~f)]
     (binding [*out* stream#]
       ~@body)))

(defn record-as-xml
  [parent file-name doc-id doc-type sentences dep-parses entity-table]
  (with-out-append-writer (File. parent file-name)
    (prxml [:doc {:id doc-id :type doc-type} 
            (for [s sentences] [:item {:id 0}])
            ])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; XML input 

;; Gigaword 

(defn gigaword-document->document
  [doc-xml]
  (let [id-and-type {:id   (:id (:attrs doc-xml))
                     :type (:type (:attrs doc-xml))}
        text-vec (->> (:content doc-xml)
                   (filter #(= (:tag %) :TEXT)) ;we only want text not head or datelines
                   (first) (:content))
        text (. (apply str (for [p text-vec] (first (:content p))))
                (replace \newline \space))]
    (assoc id-and-type :text text)))

(defn gigaword->documents
  "Given gigaword file path, gives vector of documents as maps with :type, :id, and :text."
  [file]
  (map gigaword-document->document (:content (xml/parse file))))
