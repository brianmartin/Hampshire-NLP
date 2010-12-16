(ns hampshire-nlp.rabbitmq
  (:use [rabbitcj.client]
        [clojure.contrib.duck-streams :only [file-str]]))

(def conn nil)
(def chan nil)
(def chan-name nil)

(defn init-connection
  [creds channel-name]
  (def conn (connect creds))
  (def chan (create-channel conn))
  (def chan-name channel-name))

(defn dispatch-all-file-paths
  "Put all file paths in the input directory into the queue."
  [input-dir chan-name]
  (let [files (.listFiles (file-str input-dir))]
    (declare-exchange chan chan-name fanout-exchange false false nil)
    (declare-queue chan chan-name false false true nil)
    (bind-queue chan chan-name chan-name "")
    (doall (map #(publish chan chan-name "" (.getCanonicalPath %)) files))))

(defn get-msg []
  "Get one message from the queue"
  (try (String. (.. chan (basicGet chan-name true) (getBody)))
    (catch Exception _ nil)))
