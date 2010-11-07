(ns hampshire-nlp.corpus-preprocessing.xml
  (:use [clojure.xml]
        [clojure.contrib.prxml]
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

(defn entity-table->xml
  [entity-table]
  (doall (for [e entity-table]
    [:entity {:sid    (:sid e)
              :eid    (:eid e)
              :begin  (first  (:span e))
              :end    (second (:span e))
              :phrase (:phrase e)}])))

(defn sentence-dep-parses->xml
  [dep-parses]
  (doall (for [p dep-parses]
    [:dep-parse {:dep    (:dep p)
                 :w1    (:v1 p)
                 :w2    (:v2 p)
                 :w1-i  (:v1-i p)
                 :w2-i  (:v2-i p)}])))

(defn sentences->xml
  [sentences dep-parses]
  (doall
    (map
      #(let [s-array (to-array %)]
        [:sentence
          {:id (inc %3)}
          [:parses (sentence-dep-parses->xml %2)]
          [:text (areduce s-array i ret (str "") (str ret (aget s-array i) \space))]])
    sentences dep-parses (range))))

(defn record-as-xml
  [parent file-name doc-id doc-type sentences dep-parses entity-table]
  (with-out-append-writer (File. parent file-name)
    (prxml [:doc {:id doc-id :type doc-type} 
             [:entity-table (entity-table->xml entity-table)]
             (sentences->xml sentences dep-parses)])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; XML input

;; Gigaword

(defn gigaword-document->document
  [doc-xml]
  (let [id-and-type {:id   (:id (:attrs doc-xml))
                     :type (:type (:attrs doc-xml))}
        text-vec (->> (:content doc-xml)
                   (filter #(= (:tag %) :TEXT))
                   (first) (:content))
        text (. (apply str (for [p text-vec] (first (:content p))))
                (replace \newline \space))]
    (assoc id-and-type :text text)))

(defn gigaword->documents
  "Given gigaword file path, gives vector of documents as maps with :type, :id, and :text."
  [file]
  (map gigaword-document->document (:content (parse file))))
