(defproject opiskelijavalinnat-utils/clj-test-utils "0.5.6-SNAPSHOT"
            :description "oph clojure testing utilities"
            :url "http://example.com/FIXME"
            :license {:name "EUPL"
                      :url "http://www.osor.eu/eupl/"}
            :plugins [[lein-modules "0.3.11"]]
            :dependencies [[oph/clj-s3 "0.2.5-SNAPSHOT"]
                           [oph/clj-log "0.3.1-SNAPSHOT"]
                           [com.amazonaws/aws-java-sdk-s3 "1.11.978"]
                           [io.findify/s3mock_2.12 "0.2.6"]
                           [base64-clj "0.1.1"]
                           [org.testcontainers/testcontainers "1.17.6"]
                           [org.testcontainers/elasticsearch "1.17.6"]
                           [robert/hooke "1.3.0"]]
            :deploy-repositories [["snapshots" {:url           "https://maven.pkg.github.com/Opetushallitus/opiskelijavalinnat-utils"
                                                :username      "private-token"
                                                :password      :env/GITHUB_TOKEN
                                                :sign-releases false
                                                :checksum      :ignore
                                                :releases      false
                                                :snapshots     true}]
                                  ["releases" {:url           "https://maven.pkg.github.com/Opetushallitus/opiskelijavalinnat-utils"
                                               :username      "private-token"
                                               :password      :env/GITHUB_TOKEN
                                               :sign-releases false
                                               :checksum      :ignore
                                               :releases      true
                                               :snapshots     false}]])
