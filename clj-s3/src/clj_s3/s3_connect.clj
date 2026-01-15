(ns clj-s3.s3-connect
    (:import (com.amazonaws.services.s3 AmazonS3ClientBuilder)
      (com.amazonaws.services.s3.model ObjectMetadata PutObjectRequest ListObjectsV2Request DeleteObjectsRequest)
      (java.io ByteArrayInputStream)))

(declare s3-region)
(declare s3-bucket)

(def s3-client (atom nil))

(defn- get-standard-client []
       (-> (AmazonS3ClientBuilder/standard)
           (.withRegion s3-region)
           (.build)))

(defn init-s3-client [] (reset! s3-client (get-standard-client)))

(defn- gen-path [& path-parts] (clojure.string/join "/" path-parts))
(defn- gen-key [filename & path-parts] (str (apply gen-path path-parts) "/" filename))

(defn upload [bytes mimetype filename & path-parts]
      (let [key (apply gen-key filename path-parts)
            metadata (new ObjectMetadata)]
        (.setContentLength metadata (count bytes))
        (.setContentType metadata mimetype)
        (let [request (new PutObjectRequest s3-bucket key (new ByteArrayInputStream bytes) metadata)]
          (some? (.putObject @s3-client request))
          key)))

(defn list-keys [& path-parts]
      (let [request (-> (new ListObjectsV2Request)
                        (.withBucketName s3-bucket)
                        (.withPrefix (apply gen-path path-parts)))]
           (map #(.getKey %) (.getObjectSummaries (.listObjectsV2 @s3-client request)))))

(defn delete [keys]
      (let [request (-> (new DeleteObjectsRequest s3-bucket)
                        (.withKeys (into-array keys)))]
           (.size (.getDeletedObjects (.deleteObjects @s3-client request)))))

(defn- get-object [key] (.getObject @s3-client s3-bucket key))

(defn download
  ([key]
    (let [s3-object (get-object key)
          s3-metadata (.getObjectMetadata s3-object)]
         { :stream (.getObjectContent s3-object)
          :content-type (.getContentType s3-metadata)
          :content-length (.getContentLength s3-metadata)
          :content-encoding (.getContentEncoding s3-metadata)
          :key key }))
  ([filename & path-parts] (download (apply gen-key filename path-parts))))



