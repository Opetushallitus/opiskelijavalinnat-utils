(defproject opiskelijavalinnat-utils/clj-ring-db-cas-session "1.0.0-SNAPSHOT"
  :description "Clojure-kirjasto Opintoplun CAS-kirjautumiseen ja session tallentamiseen tietokantaan."
  :url "http://example.com/FIXME"
  :license {:name "EUPL, Version 1.1 or - as soon as they will be approved by the European Commission - subsequent versions of the EUPL (the \"Licence\")"
            :url  "http://www.osor.eu/eupl/"}

  :managed-dependencies [[com.fasterxml.jackson.core/jackson-databind "2.20.0"]
                         [com.fasterxml.jackson.core/jackson-core "2.20.0"]
                         [org.apache.commons/commons-fileupload2-core "2.0.0-M4"]]

  :dependencies [[org.clojure/clojure "1.12.2"]
                 [org.clojure/data.json "2.5.1"]
                 [ring/ring-core "1.11.0"]
                 [buddy/buddy-auth "3.0.323"]
                 [yesql "0.5.4"]]

  :plugins [[lein-modules "0.3.11"]
            [lein-ancient "0.7.0"]
            [lein-less "1.7.5"]
            [lein-shell "0.5.0"]]

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
                                      :snapshots     false}]]

  :min-lein-version "2.5.3")
