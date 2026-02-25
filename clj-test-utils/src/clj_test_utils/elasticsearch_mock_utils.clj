(ns clj-test-utils.elasticsearch-mock-utils
    (:require
      [clj-test-utils.port-finder :refer [find-free-local-port]]
      [robert.hooke :refer [add-hook]])
  (:import
    (pl.allegro.tech.embeddedelasticsearch EmbeddedElastic PopularProperties)
    (java.util.concurrent TimeUnit)))

(def embedded-elastic (atom nil))

(defn start-embedded-elasticsearch
  ([port timeoutInMillis]
    (reset! embedded-elastic (-> (EmbeddedElastic/builder)
                                 (.withElasticVersion "6.8.4")
                                 (.withSetting PopularProperties/HTTP_PORT port)
                                 (.withSetting PopularProperties/CLUSTER_NAME "elasticsearch")
                                 (.withSetting "discovery.zen.ping.unicast.hosts" (java.util.ArrayList. [(str "127.0.0.1:" port)]))
                                 (.withSetting "action.auto_create_index" ".watches,.triggered_watches,.watcher-history-*")
                                 (.withStartTimeout timeoutInMillis TimeUnit/MILLISECONDS)
                                 (.build)))
    (.start @embedded-elastic))
  ([port]
    (start-embedded-elasticsearch port (* 60 1000))))


(defn stop-elastic-test []
      (.stop @embedded-elastic))

(defn init-elastic-test []
      (let [port (find-free-local-port)]
           (intern 'clj-elasticsearch.elastic-utils 'elastic-host (str "http://127.0.0.1:" port))
           (start-embedded-elasticsearch port)))

(defn mock-embedded-elasticsearch-fixture [test]
      (init-elastic-test)
      (test)
      (stop-elastic-test))

(defn global-elasticsearch-fixture
  []
  (defn- run-all-test-hook
    [f & nss]
    (let [embedded-elasticsearch? (find-ns 'clj-elasticsearch.elastic-utils)]
      (when embedded-elasticsearch? (init-elastic-test))
        (let [result (apply f nss)]
          (when embedded-elasticsearch? (stop-elastic-test))
          result)))
    (add-hook #'clojure.test/run-tests #'run-all-test-hook))
