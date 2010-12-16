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
  [file output-dir count-method]
  (println (.getName file))
  (let [merged-count-map (atom {})
        total (atom 0)]
    (doseq [document (x/file->documents file)]
      (let [entity-table (x/document->entity-table document)
            parses (x/document->parses document)
            entity-resolved-parses (c/count-occurences entity-table parses)
            count-map (make-count-map entity-resolved-parses count-method)
            single-total (cl/total-pair-count count-map)]
        (reset! merged-count-map (merge-two-count-maps @merged-count-map count-map))
        (reset! total (+ single-total @total))))
    (x/record-count-map-as-xml
      (File. output-dir (str (.getName file) "_count_map.xml"))
      (.getName file) @merged-count-map @total count-method)))

(defn process-off-queue
  "Processes one file off of the queue."
  [output-dir count-method]
  (let [msg (get-msg)]
    (if (not (nil? msg))
      (process (File. msg) output-dir count-method))))

(defn merge-xml-count-maps
  "Merges count-map xml files in the input-dir into a single set of counts
  saved to the same directory as xml."
  [input-dir count-method]
  (let [files (map #(.getCanonicalPath %) (.listFiles (file-str input-dir)))
        mega-count-map (c/merge-count-map-vector (map xml->counts files))]
    (x/record-count-map-as-xml
      (File. input-dir "mega_merged_count_map.xml")
       nil mega-count-map (cl/total-pair-count mega-count-map) count-method)))

(defn start-worker
  "Process files from the queue (if none, waits 5 seconds, recheck... forever)."
  [output-dir count-method]
  (while true
    (process-off-queue output-dir count-method)
    (Thread/sleep 5000)))

(defn run
  "Takes preprocessed xml files in an input directory, performs counting and writes
  counts to the output directory in xml convenient for further processing."
  [{input-dir :input-dir output-dir :output-dir count-method :count-method
    job-dist? :job-dist?  debug? :debug? mega-merge? :mega-merge?
    chan :chan host :host user :user pass :pass port :port}]

  (init-connection {:username user :password pass
                    :virtual-host "/" :host host :port port} chan)

  (cond job-dist?   (dispatch-all-file-paths input-dir chan)
        mega-merge? (merge-xml-count-maps (file-str input-dir) count-method)
        debug?      (process-off-queue (file-str output-dir) count-method)
        :else       (start-worker (file-str output-dir) count-method)))
