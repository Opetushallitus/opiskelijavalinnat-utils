(defproject opiskelijavalinnat-utils/clj-log "0.3.2-SNAPSHOT"
            :description "oph clojure logging utilities"
            :url "http://example.com/FIXME"
            :license {:name "EUPL"
                      :url "http://www.osor.eu/eupl/"}
            :plugins [[lein-modules "0.3.11"]]
            :dependencies [[org.clojure/tools.logging "1.1.0"]
                           [clj-time "0.15.2"]
                           [cheshire "5.10.0"]
                           [slingshot "0.12.2"]]
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
