(ns clj-test-utils.s3-mock-utils
    (:require [clj-s3.s3-connect :as s3]
      [clj-test-utils.port-finder :refer [find-free-local-port]])
    (:import (io.findify.s3mock S3Mock)
      (com.amazonaws.client.builder AwsClientBuilder$EndpointConfiguration)
      (com.amazonaws.services.s3 AmazonS3ClientBuilder)
      (com.amazonaws.auth AWSStaticCredentialsProvider AnonymousAWSCredentials)
      (com.amazonaws.services.s3.model CreateBucketRequest)))

(intern 'clj-s3.s3-connect 's3-region "eu-west-1")
(intern 'clj-s3.s3-connect 's3-bucket "buketti")

(def ^:private mock (atom nil))

(defn init-s3-mock []
    (let [port (find-free-local-port)
          s3-url (str "http://localhost:" port)]
         (reset! mock (S3Mock/create port))
         (.start @mock)
         (let [endpoint-config (new AwsClientBuilder$EndpointConfiguration s3-url, s3/s3-region)
               client (-> (AmazonS3ClientBuilder/standard)
                          (.withPathStyleAccessEnabled true)
                          (.withEndpointConfiguration endpoint-config)
                          (.withCredentials (new AWSStaticCredentialsProvider (new AnonymousAWSCredentials)))
                          (.build))]
              (reset! s3/s3-client client)
              (.createBucket @s3/s3-client (new CreateBucketRequest s3/s3-bucket s3/s3-region)))))

(defn stop-s3-mock [] (.stop @mock))

(defn mock-s3-fixture [f]
    (init-s3-mock)
    (f)
    (stop-s3-mock))