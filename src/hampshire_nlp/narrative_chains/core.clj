(ns hampshire-nlp.narrative-chains.core
  (:use [hampshire-nlp.rabbitmq]
        [hampshire-nlp.narrative-chains.xml :as x]
        [hampshire-nlp.narrative-chains.counting :as c]
        [hampshire-nlp.narrative-chains.clustering :as cl]
        [clojure.contrib.duck-streams :only [file-str]])
  (:import [java.io File]))

(def count-map-vector '())

(defn process
  "Produces a count-map on the file given and writes output to xml
  in the output-dir."
  [file output-dir]
  (let [documents (x/file->documents file)]
    (doseq [document documents]
      (let [entity-table (x/document->entity-table document)
            parses (x/document->parses document)
            entity-resolved-parses (c/count-occurences entity-table parses)]
        (def count-map-vector (conj count-map-vector (make-count-map entity-resolved-parses))))))
  (def merged-count-map (merge-count-map-vector count-map-vector))
  (def counts {:ind (cl/individual-totals merged-count-map)
               :pair (cl/pair-totals merged-count-map)}))

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
