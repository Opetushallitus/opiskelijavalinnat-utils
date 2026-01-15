(defproject opiskelijavalinnat-utils/clj-log "0.3.2-SNAPSHOT"
            :description "oph clojure logging utilities"
            :url "http://example.com/FIXME"
            :license {:name "EUPL"
                      :url "http://www.osor.eu/eupl/"}
            :plugins [[lein-modules "0.3.11"]]
            :dependencies [[org.clojure/tools.logging "1.3.1"]
                           [clj-time "0.15.2"]
                           [cheshire "5.13.0"]
                           [slingshot "0.12.2"]]
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
