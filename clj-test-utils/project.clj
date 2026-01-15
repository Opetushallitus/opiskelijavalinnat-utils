(defproject opiskelijavalinnat-utils/clj-test-utils "0.5.7-SNAPSHOT"
            :description "oph clojure testing utilities"
            :url "http://example.com/FIXME"
            :license {:name "EUPL"
                      :url "http://www.osor.eu/eupl/"}
            :plugins [[lein-modules "0.3.11"]]
            :dependencies [[opiskelijavalinnat-utils/clj-s3 "0.2.5-SNAPSHOT"]
                           [opiskelijavalinnat-utils/clj-log "0.3.1-SNAPSHOT"]
                           [com.amazonaws/aws-java-sdk-s3 "1.11.978"]
                           [io.findify/s3mock_2.12 "0.2.6"]
                           [base64-clj "0.1.1"]
                           [org.testcontainers/testcontainers "2.0.1"]
                           [org.testcontainers/testcontainers-elasticsearch "2.0.1"]
                           [robert/hooke "1.3.0"]]
            :repositories [["github" {:url "https://maven.pkg.github.com/Opetushallitus/packages"
                                      :username "private-token"
                                      :password :env/GITHUB_TOKEN}]
                           ["releases" {:url           "https://artifactory.opintopolku.fi/artifactory/oph-sade-release-local"
                                        :sign-releases false
                                        :snapshots     false}]
                           ["snapshots" {:url      "https://artifactory.opintopolku.fi/artifactory/oph-sade-snapshot-local"
                                         :releases {:update :never}}]
                           ["ext-snapshots" {:url      "https://artifactory.opintopolku.fi/artifactory/ext-snapshot-local"
                                             :releases {:update :never}}]]
            :deploy-repositories [["snapshots" {:url           "https://maven.pkg.github.com/Opetushallitus/packages"
                                                :username      "private-token"
                                                :password      :env/GITHUB_TOKEN
                                                :sign-releases false
                                                :checksum      :ignore
                                                :releases      false
                                                :snapshots     true}]
                                  ["releases" {:url           "https://maven.pkg.github.com/Opetushallitus/packages"
                                               :username      "private-token"
                                               :password      :env/GITHUB_TOKEN
                                               :sign-releases false
                                               :checksum      :ignore
                                               :releases      true
                                               :snapshots     false}]])
