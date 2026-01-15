(ns clj-elasticsearch.elastic-utils
    (:require [clj-http.client :as http]
              [cheshire.core :as json]
              [clojure.string :refer [join]]))

(declare elastic-host)

(defonce timeout 120000)

(defn elastic-url
  [& parts]
  (str elastic-host (apply str (map #(str "/" %) parts))))

(defn- json-request
  [body]
  {:body (if (instance? String body) body (json/encode body))
   :as :json
   :content-type :json
   :socket-timeout timeout})

(defn- merge-query-params
  [request query-params]
  (cond-> request (seq (keys query-params)) (merge {:query-params query-params})))

(defn elastic-post
  ([url body query-params parseBody?]
   (cond-> (http/post url (-> body json-request (merge-query-params query-params)))
           parseBody? (:body)))
  ([url body query-params]
   (elastic-post url body query-params true))
  ([url body]
   (elastic-post url body {} true)))

(defn elastic-put
  ([url body query-params parseBody?]
   (cond-> (http/put url (-> body json-request (merge-query-params query-params)))
           parseBody? (:body)))
  ([url body query-params]
   (elastic-put url body query-params true))
  ([url body]
   (elastic-put url body {} true)))

(defn elastic-get
  ([url body query-params parseBody?]
   (cond-> (http/get url (-> body json-request (merge-query-params query-params)))
           parseBody? (:body)))
  ([url query-params parseBody?]
   (cond-> (http/get url (merge-query-params {:socket-timeout timeout :as :json} query-params))
            parseBody? (:body)))
  ([url query-params]
   (elastic-get url query-params true))
  ([url]
   (elastic-get url {} true)))

(defn elastic-delete
  [url]
  (http/delete url {:content-type :json :socket-timeout timeout}))

;MAX request payload size in AWS ElasticSearch
(defonce max-payload-size 10485760)

(defn bulk-partitions
  [data]
  (let [action? (fn [d] (or (contains? d :index) (contains? d :update) (contains? d :create) (contains? d :delete)))
        action-counter (atom 0)
        action-nr (fn [d] (if (action? d)
                            (swap! action-counter inc)
                            @action-counter))
        encode (fn [a] (join (map #(str (json/encode %) "\n") a)))
        cur-bytes (atom 0)
        partitioner (fn [e]
                      (let [bytes (count (.getBytes e))]
                        (if (> max-payload-size (+ @cur-bytes bytes))
                          (do (reset! cur-bytes (+ @cur-bytes bytes)) true)
                          (do (reset! cur-bytes 0) false))))]

    (->> data
         (partition-by action-nr)
         (map encode)
         (partition-by partitioner)
         (map #(clojure.string/join %)))))

(defn elastic-empty? []
  (let [url (elastic-url "_all" "_count")
        count (:count (elastic-get url))]
    (= count 0)))