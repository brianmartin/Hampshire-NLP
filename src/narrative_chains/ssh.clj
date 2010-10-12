(ns narrative-chains.ssh
  (:use clj-ssh.ssh))

(defn run-command
  [cmd host]
  (with-ssh-agent []
    (let [s (session host :strict-host-key-checking :no)]
      (with-connection session
          (ssh session :in cmd)))))

(defn run-commands
  [cmd hosts]
  (map #(future (run-command cmd %)) hosts))
