(ns hampshire-nlp.dispatch
  (:use [rabbitcj.client]
        [clojure.contrib.duck-streams :only [file-str]]))

(def conn nil)
(def chan nil)

(defn init-connection
  [creds]
  (def conn (connect creds))
  (def chan (create-channel conn)))

(defn dispatch-all-file-paths
  "Put all file paths in the input directory into the queue."
  [input-dir]
  (let [files (.listFiles (file-str input-dir))]
    (declare-exchange chan "nlp" fanout-exchange false false nil)
    (declare-queue chan "nlp" false false true nil)
    (bind-queue chan "nlp" "nlp" "")
    (doall (map #(publish chan "nlp" "" (.getCanonicalPath %)) files))))

(defn get-msg []
  (try (String. (.. chan (basicGet "nlp" true) (getBody)))
    (catch Exception _ nil)))
