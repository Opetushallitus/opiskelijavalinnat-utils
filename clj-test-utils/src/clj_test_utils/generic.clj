(ns clj-test-utils.generic)

(defn run-proc [proc-name & args]
  (.waitFor (-> (ProcessBuilder. (into-array String (concat [proc-name] args))) .inheritIO .start)))
