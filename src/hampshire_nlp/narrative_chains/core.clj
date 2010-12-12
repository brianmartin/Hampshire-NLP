(ns hampshire-nlp.narrative-chains.core
  (:use [hampshire-nlp.rabbitmq]
        [hampshire-nlp.narrative-chains.xml :as x]
        [hampshire-nlp.narrative-chains.counting :as c]
        [hampshire-nlp.narrative-chains.clustering :as cl]
        [clojure.contrib.duck-streams :only [file-str]]
        [clojure.contrib.pprint])
  (:import [java.io File]))

(defn process
  "Produces a count-map on the file given and writes output to xml
  in the output-dir."
  [file output-dir]
  (println (.getName file))
  (let [merged-count-map (atom {})
        total (atom 0)
        count-method :word-pair]
    (doseq [document (x/file->documents file)]
      (let [entity-table (x/document->entity-table document)
            parses (x/document->parses document)
            entity-resolved-parses (c/count-occurences entity-table parses)
            count-map (make-count-map entity-resolved-parses count-method)
            single-total (cl/total-pair-count count-map)]
        (reset! merged-count-map (merge-two-count-maps @merged-count-map count-map))
        (reset! total (+ single-total @total))))
    (x/record-count-map-as-xml (File. output-dir (str (.getName file) "_count_map.xml")) (.getName file) @merged-count-map @total count-method)))

(defn process-off-queue
  "Processes one file off of the queue."
  [output-dir]
  (let [msg (get-msg)]
    (if (not (nil? msg))
      (process (File. msg) output-dir))))

(defn start-worker
  "Process files from the queue (if none, waits 5 seconds, recheck... forever)."
  [output-dir]
  (while true
    (process-off-queue output-dir)
    (Thread/sleep 5000)))

(defn run
  "Parses files in an input directory, performs coref, and writes results
  to the output directory in xml convenient for further processing."
  [{input-dir :input-dir output-dir :output-dir
    job-dist? :job-dist?  debug? :debug?
    host :host user :user pass :pass port :port}]

  (init-connection {:username user :password pass
                    :virtual-host "/" :host host :port port})

  (if job-dist?
    (dispatch-all-file-paths input-dir)
    (start-worker (file-str output-dir)))

  (if debug?
    (process-off-queue (file-str output-dir))))
