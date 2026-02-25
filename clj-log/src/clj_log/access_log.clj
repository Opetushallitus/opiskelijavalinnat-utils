(ns clj-log.access-log
    (:require [clojure.tools.logging.impl :as impl]
      [clj-log.access-headers :refer :all]
      [clj-time.core :as t]
      [cheshire.core :as c]))

(declare service)

(def ^{:private true} logger (impl/get-logger (impl/find-factory) "ACCESS"))

(defn parse-access-headers [start request response]
      (let [duration (- (System/currentTimeMillis) start)
            method (get-method-from-request request)
            path-info (request :uri)]
           {:user-agent    (user-agent-from-request request)
            :remote-addr   (remote-addr-from-request request)
            :timestamp     (t/now)
            :customer      "OPH"
            :service       service
            :caller-id     (caller-id-from-request request)
            :responseCode  (response :status)
            :request       (str method " " path-info)
            :requestMethod method
            :responseTime  (str duration)}))

(defn log-access [start request response]
  (.info logger (c/generate-string (parse-access-headers start request response))))

(defmacro with-access-logging [request & operations]
          `(let [start# (System/currentTimeMillis)
                 response# ~(cons 'do operations)]
                (log-access start# ~request response#)
                response#))