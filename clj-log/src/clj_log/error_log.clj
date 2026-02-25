(ns clj-log.error-log
    (:require [clojure.tools.logging.impl :as impl]
              [slingshot.slingshot :refer [try+]]))

(defmacro with-error-logging-value
  [value & body]
  `(try+
     (do ~@body)
     (catch #(and (map? %) (contains? % :status)) {:keys [~'trace-redirects ~'status ~'body]}
       (.error (impl/get-logger (impl/find-factory) *ns*) (str "HTTP " ~'status " from: " ~'trace-redirects " [" ~'body "]"))
       ~value)
     (catch Object ~'_
       (.error (impl/get-logger (impl/find-factory) *ns*) "Error: " (:throwable ~'&throw-context))
       ~value)))

(defmacro with-error-logging
  [& body]
  `(with-error-logging-value nil ~@body))