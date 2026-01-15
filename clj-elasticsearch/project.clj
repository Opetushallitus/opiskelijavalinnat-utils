(defproject opiskelijavalinnat-utils/clj-elasticsearch "0.5.4-SNAPSHOT"
            :description "oph clojure elasticsearch utilities"
            :url "http://example.com/FIXME"
            :license {:name "EUPL"
                      :url "http://www.osor.eu/eupl/"}
            :plugins [[lein-modules "0.3.11"]]
            :dependencies [[clj-http "3.12.1"]
                           [cheshire "5.10.0"]]
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
