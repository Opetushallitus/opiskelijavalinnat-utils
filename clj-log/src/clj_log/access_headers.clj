(ns clj-log.access-headers)

(defn- if-or [& args]
       (if-let [a (first args)]
               a
               (if-let [tail (not-empty (rest args))]
                       (apply if-or tail))))

(defn- find-first [m & keys]
       (when-let [f (first keys)]
                 (if-let [v (m f)]
                         v
                         (apply find-first (into [m] (rest keys))))))

(defn get-method-from-request [request]
      (let [conversion-table {:get "GET"
                              :options "OPTIONS"
                              :post "POST"
                              :put "PUT"
                              :delete "DELETE"
                              :head "HEAD"
                              :connect "CONNECT"
                              :trace "TRACE"}]
           (get conversion-table (request :request-method))))

(defn user-agent-from-request [request]
      ((request :headers) "user-agent"))

(defn caller-id-from-request [request]
  ((request :headers) "caller-id"))

(defn remote-addr-from-request [request]
      (if-or
        (find-first (request :headers) "x-real-ip" "x-forwarded-for")
        (request :remote-addr)))