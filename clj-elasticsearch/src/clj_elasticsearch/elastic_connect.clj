(ns clj-elasticsearch.elastic-connect
    (:refer-clojure :exclude [count])
    (:require [clj-elasticsearch.elastic-utils :refer :all]
              [clj-http.client :as http]))

(defn get-cluster-health
  []
  (elastic-get (elastic-url "_cluster" "health") {} false))

(defn check-elastic-status
  []
  (-> (get-cluster-health)
      :status
      (= 200)))

(defn get-indices-info
  []
  (-> (elastic-get (elastic-url "_cat" "indices") {:v "" :format "json"})))

(defn get-elastic-status
  []
  {:cluster_health (:body (get-cluster-health))
   :indices-info (get-indices-info)})

(defn create
  [index document]
  (elastic-post (elastic-url index) document))

(defn create-index
  ([index settings mappings]
   (let [json (cond-> {:settings settings}
                      (seq (keys mappings)) (merge {:mappings mappings}))]
     (elastic-put (elastic-url index) json)))
  ([index settings]
   (create-index index settings nil)))

(defn search
  [index & query-params]
  (let [query-map (apply array-map query-params)]
    (elastic-post (elastic-url index "_search") query-map)))

(defn simple-search
  ([index query pretty]
    (elastic-get (elastic-url index "_search") { :q query :pretty pretty}))
  ([index query]
    (simple-search index query true)))

(defn count
  [index & query-params]
  (let [query-map (apply array-map query-params)]
    (:count (elastic-post (elastic-url index "_count") query-map))))

(defn parse-search-result
  [res]
  (map :_source (get-in res [:hits :hits])))

(defn get-document
  [index id & query-params]
  (let [query-map (apply array-map query-params)]
    (try
      (elastic-get (elastic-url index "_doc" id) query-map)
      (catch Exception e
        (if (= 404 (some-> e (ex-data) :status)) {:found false} (throw e))))))

(defn multi-get
  [index ids & query-params]
  (->> (elastic-get (elastic-url index "_mget") {:ids (vec ids)} (apply array-map query-params) true)
       :docs
       (filter :found)
       (map :_source)
       (vec)))

(defn bulk
  [index data]
  (when (seq data)
    (let [bulk-url   (elastic-url index "_bulk")
          partitions (bulk-partitions data)]
      (for [partition partitions]
        (elastic-post bulk-url partition)))))

(defn index-exists
  [index]
  (try
    (-> (http/head (elastic-url index))
        (:status)
        (= 200))
    (catch Exception e
      (if (= 404 ((ex-data e) :status)) false (throw e)))))

(defn refresh-index
  [index]
  (http/post (elastic-url index "_refresh") {:content-type :json}))

(defn delete-index ; TODO -connection timeouts
  [index]
  (try
    (elastic-delete (elastic-url index))
    (catch Exception e
      (if (not (= 404 ((ex-data e) :status))) (throw e)))))

(defn move-alias
  ([alias index write-index?]
   (elastic-post (elastic-url "/_aliases") {:actions [{:remove {:index "*" :alias alias}},
                                                      {:add {:index index :alias alias :is_write_index write-index?}}]}))
  ([alias index]
   (move-alias alias index false)))

(defn list-indices-with-alias
  [alias]
  (try
    (let [response (elastic-get (elastic-url "/_alias" alias))]
      (vec (for [index (keys response)]
             {:index (name index)
              :is_write_index (get-in response [index :aliases (keyword alias) :is_write_index])})))
    (catch Exception e
      (if (= 404 (:status (ex-data e))) [] (throw e)))))

(defn find-write-index
  [alias]
  (when-let [write-index (first (filter #(:is_write_index %) (list-indices-with-alias alias)))]
    (:index write-index)))

(defn list-aliases
  []
  (try
    (elastic-get (elastic-url "_aliases"))
    (catch Exception e
      (if (= 404 ((ex-data e) :status)) {:found false} (throw e)))))

(defn move-read-alias-to-write-index
  [write-alias read-alias]
  (when-let [write-index (find-write-index write-alias)]
    (move-alias read-alias write-index)
    write-index))
