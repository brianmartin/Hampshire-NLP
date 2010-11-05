(defproject hampshire-nlp "0.0.1"
  :description "NLP tools including: corpus pre-processing, clustering, narrative chain extraction, and relation extraction."
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [org.clojars.zaxtax/opennlp-tools "1.5.0"]
                 [edu.stanford.nlp/stanford-parser "1.6.2"]
                 [org.clojars.adityo/clj-tagsoup "0.1.2"]
                 [clj-ssh "0.2.0-SNAPSHOT"]
                 [rabbitcj "0.1.0-SNAPSHOT"]]
  :main hampshire-nlp.core
  :java-opts ["-Xms3g" "-Xmx5G"])
