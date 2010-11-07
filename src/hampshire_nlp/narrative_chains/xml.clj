(ns hampshire-nlp.narrative-chains.xml
  (:require [clojure.xml :as x]
            [clojure.zip :as z]
            [clojure.contrib.zip-filter.xml :as zf]))

(defn i [s] (Integer. s))

(defn file->documents
  [file]
  (:content (x/parse file)))

(defn document->parses
  [document]
  (let [zipper (z/xml-zip document)
        deps   (zf/xml-> zipper :sentence :parses :dep-parse (zf/attr :dep))
        w1s    (zf/xml-> zipper :sentence :parses :dep-parse (zf/attr :w1))
        w2s    (zf/xml-> zipper :sentence :parses :dep-parse (zf/attr :w2))
        w1-is  (zf/xml-> zipper :sentence :parses :dep-parse (zf/attr :w1-i))
        w2-is  (zf/xml-> zipper :sentence :parses :dep-parse (zf/attr :w2-i))]
    (filter #(not= nil %)
      (map #(try {:dep % :w1 %2 :w2 %3 :w1-i (i %4) :w2-i (i %5)}
              (catch java.lang.NumberFormatException _ nil))
           deps w1s w2s w1-is w2-is))))

(defn document->entity-table
  [document]
  (let [zipper (z/xml-zip document)
        sids     (zf/xml-> zipper :entity-table :entity (zf/attr :sid))
        eids     (zf/xml-> zipper :entity-table :entity (zf/attr :eid))
        begins   (zf/xml-> zipper :entity-table :entity (zf/attr :begin))
        ends     (zf/xml-> zipper :entity-table :entity (zf/attr :end))]
    (filter #(not= nil %) 
      (map #(try {:sid (i %) :eid (i %2) :begin (i %3) :end (i %4)}
              (catch java.lang.NumberFormatException _ nil))
           sids eids begins ends))))
