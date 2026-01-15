(defproject opiskelijavalinnat-utils/clj-s3 "0.2.6-SNAPSHOT"
            :description "oph clojure s3 utilities"
            :url "http://example.com/FIXME"
            :license {:name "EUPL"
                      :url "http://www.osor.eu/eupl/"}
            :plugins [[lein-modules "0.3.11"]]
            :dependencies [[com.amazonaws/aws-java-sdk-s3 "1.11.978"]]
            :profiles { :test { :dependencies [[opiskelijavalinnat-utils/clj-test-utils "0.5.7-SNAPSHOT"]
                                               [base64-clj "0.1.1"]]}}
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
