(ns hampshire-nlp.narrative-chains.xml
  (:use [clojure.xml :as x]
        [clojure.zip :as z]
        [clojure.contrib.zip-filter.xml :as zf]
        [clojure.contrib.prxml]
        [clojure.contrib.duck-streams :only [with-out-append-writer]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; XML -> clojure data

;; preprocessed-corpus -> XML

(defn i [s] (Integer. s))

(defn file->documents
  [file]
  (:content (x/parse file)))

(defn document->parses
  [document]
  (let [zipper (z/xml-zip document)
        deps   (zf/xml-> zipper :sentence :parses :dep-parse (zf/attr :dep))
        sids   (zf/xml-> zipper :sentence :parses :dep-parse (zf/attr :sid))
        w1s    (zf/xml-> zipper :sentence :parses :dep-parse (zf/attr :w1))
        w2s    (zf/xml-> zipper :sentence :parses :dep-parse (zf/attr :w2))
        w1-is  (zf/xml-> zipper :sentence :parses :dep-parse (zf/attr :w1-i))
        w2-is  (zf/xml-> zipper :sentence :parses :dep-parse (zf/attr :w2-i))]
    (filter #(not= nil %)
      (map #(try {:dep % :sid (i %2) :w1 %3 :w2 %4 :w1-i (i %5) :w2-i (i %6)}
              (catch java.lang.NumberFormatException _ nil))
           deps sids w1s w2s w1-is w2-is))))

(defn document->entity-table
  [document]
  (let [zipper (z/xml-zip document)
        sids   (zf/xml-> zipper :entity-table :entity (zf/attr :sid))
        eids   (zf/xml-> zipper :entity-table :entity (zf/attr :eid))
        begins (zf/xml-> zipper :entity-table :entity (zf/attr :begin))
        ends   (zf/xml-> zipper :entity-table :entity (zf/attr :end))]
    (filter #(not= nil %) 
      (map #(try {:sid (i %) :eid (i %2) :begin (i %3) :end (i %4)}
              (catch java.lang.NumberFormatException _ nil))
           sids eids begins ends))))

;; count-map
(defn xml->counts [file]
  (let [data  (:content (x/parse file))
        total (i (.trim (first (:content (first data)))))
        counts (map :attrs (:content (second data)))]
     (into {}
       (for [c counts]
         (if (:w2 c)
           [[{:word (:w1 c) :dep (:w1-dep c)} {:word (:w2 c) :dep (:w2-dep c)}]
            {:cnt (i (:cnt c))}]
           [[{:word (:w1 c) :dep (:w1-dep c)}]
            {:cnt (i (:cnt c))}])))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; clojure data -> XML

(defn counts->xml
  [count-map method]
  (let [counts (filter #(not= 0 (:cnt (second %))) count-map)]
    (cond (= method :word-pair)
              (doall (for [cnt counts]
                (let [words (first cnt)]
                  [:count {:w1 (first words)
                           :w2 (second words)
                           :cnt (:cnt (second cnt))}])))
          (= method :word-pair-and-dep)
              (doall (for [c counts]
                (let [words-and-deps (first c)]
                  [:count {:w1 (:word (first words-and-deps))
                           :w1-dep (:dep (first words-and-deps))
                           :w2 (:word (second words-and-deps))
                           :w2-dep (:dep (second words-and-deps))
                           :cnt (:cnt (second c))}])))
          (= method :word) ;TODO
              nil)))

(defn record-count-map-as-xml
  [output-file id count-map total count-method]
  (with-out-append-writer output-file
    (prxml [:root {:id id :count-method count-method}
            [:total total]
            [:counts (counts->xml count-map count-method)]])))
