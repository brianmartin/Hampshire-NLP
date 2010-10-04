(defproject narrative-chains "0.0.1"
  :description "Clojure wrapper for the Stanford Parser (distributed using rabbitmq)."
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [org.clojars.pjt/opennlp-tools "1.4.3"]
                 [edu.stanford.nlp/stanford-parser "1.6.2"]
                 [rabbitcj "0.1.0-SNAPSHOT"]]
  :main narrative-chains.core)
