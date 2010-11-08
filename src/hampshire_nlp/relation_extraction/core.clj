(ns hampshire-nlp.relation-extraction.core
  (:use [hampshire-nlp.rabbitmq]
        [clojure.contrib.duck-streams :only [file-str]])
  (:import [java.io File]))

(defn process
  "Produces a count-map on the file given and writes output to xml
  in the output-dir."
  []
  nil)

(defn process-one
  "Processes one file off of the queue."
  [output-dir]
  (let [msg (get-msg)]
    (if (not (nil? msg))
      (process (File. msg) output-dir))))

(defn start-worker
  "Process files from the queue (if none, waits 10 seconds)."
  [output-dir]
  (loop [msg (get-msg)]
    (if (not (nil? msg))
      (do
        (process (File. msg) output-dir)
        (println "done " msg))
      (Thread/sleep 10000))
    (recur (get-msg))))

(defn run
  "Parses files in an input directory, performs coref, and writes results
  to the output directory in xml convenient for further processing."
  [{input-dir :input-dir
    output-dir :output-dir
    job-dist? :job-dist?
    debug? :debug?}]

  (init-connection {:username "guest" :password "guest"
                    :virtual-host "/" :host "127.0.0.1" :port 5672})

  (if job-dist?
    (dispatch-all-file-paths input-dir)
    (start-worker (file-str output-dir)))

  (if debug?
    (process-one (file-str output-dir))))
