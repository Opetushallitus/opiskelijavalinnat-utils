(ns clj-s3.s3-connect-test
    (:require [clojure.test :refer :all]
              [clj-s3.s3-connect :as s3]
              [clj-test-utils.s3-mock-utils :refer :all]
              [base64-clj.core :as b64])
    (:import (io.findify.s3mock S3Mock)
      (com.amazonaws.client.builder AwsClientBuilder$EndpointConfiguration)
      (com.amazonaws.services.s3 AmazonS3ClientBuilder)
      (com.amazonaws.auth AWSStaticCredentialsProvider AnonymousAWSCredentials)
      (com.amazonaws.services.s3.model CreateBucketRequest)))

(defonce koulutus-oid "1.2.3.4.567")

(use-fixtures :once mock-s3-fixture)

(deftest s3-connect-test
       (testing "return empty list"
             (is (= 0 (count (s3/list-keys "koulutus" koulutus-oid)))))

       (testing "upload documents"
             (s3/upload (.getBytes "moi") "text/plain" "moi.txt" "koulutus" koulutus-oid "kieli_fi")
             (s3/upload (.getBytes "hej") "text/plain" "moi.txt" "koulutus" koulutus-oid "kieli_sv")
             (s3/upload (.getBytes "terve") "text/plain" "terve.txt" "koulutus" koulutus-oid "kieli_fi")
             (is (= 3 (count (s3/list-keys "koulutus" koulutus-oid)))))

       (testing "list documents in specific language"
             (is (= 2 (count (s3/list-keys "koulutus" koulutus-oid "kieli_fi")))))

       (testing "download document by path parts"
             (let [doc (s3/download "moi.txt" "koulutus" koulutus-oid "kieli_fi")]
                (is (= "moi" (slurp (:stream doc))))
                (is (= "text/plain" (:content-type doc)))))

       (testing "download document by key"
            (let [doc (s3/download (str "koulutus/" koulutus-oid "/kieli_fi/terve.txt"))]
                 (is (= "terve" (slurp (:stream doc))))
                 (is (= "text/plain" (:content-type doc)))))

       (testing "delete documents"
             (let [resp (s3/list-keys "koulutus" koulutus-oid)]
                  (s3/delete resp))
             (is (= 0 (count (s3/list-keys "koulutus" koulutus-oid))))))