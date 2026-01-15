(ns clj-test-utils.elasticsearch-docker-utils
  (:import [org.testcontainers.elasticsearch ElasticsearchContainer]
           [org.testcontainers.utility DockerImageName])
  (:require
    [robert.hooke :refer [add-hook]]
    [clojure.java.shell :refer [sh]]))

(def image (. (. DockerImageName (parse "190073735177.dkr.ecr.eu-west-1.amazonaws.com/utility/elasticsearch-kouta:8.5.2")) asCompatibleSubstituteFor "docker.elastic.co/elasticsearch/elasticsearch"))
(def elastic (delay (new ElasticsearchContainer image)))

(defn stop-elasticsearch []
  (println "Stopping elasticsearch container")
  (.stop @elastic))

(defn- elastic-has-started? [elastic-ip]
  (let [response (sh "curl" (str elastic-ip "/_cluster/health"))
        result-code (:exit response)]
    (= result-code 0)))

(defn- wait-elastic-to-start [elastic-ip]
  (loop [tries 30]
    (when (and (> tries 1)
               (not (elastic-has-started? elastic-ip)))
      (Thread/sleep 1000)
      (recur (- tries 1)))))

(defn start-elasticsearch []
  (println "Starting elasticsearch container")
  (doto (.getEnvMap @elastic)
    (.put "xpack.security.enabled" "false")
    (.put "ES_JAVA_OPTS" "-Xms2g -Xmx2g"))
  (.start @elastic)
  (let [port (.getMappedPort @elastic 9200)
        elastic-ip (str "http://127.0.0.1:" port)]
    (wait-elastic-to-start elastic-ip)
    (intern 'clj-elasticsearch.elastic-utils 'elastic-host elastic-ip)
    (println "Elasticsearch container started")))

(defn global-docker-elastic-fixture
  [& args]
  (defn- run-tests-hook
    [f & nss]
    (let [embedded-elasticsearch? (find-ns 'clj-elasticsearch.elastic-utils)]
      (when embedded-elasticsearch? (start-elasticsearch))
      (let [result (apply f nss)]
        (when (and embedded-elasticsearch? (not (:dont-stop-elasticsearch args))) (stop-elasticsearch))
        result)))
  (add-hook #'clojure.test/run-tests #'run-tests-hook))
