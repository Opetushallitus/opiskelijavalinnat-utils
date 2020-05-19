(defproject oph/clj-ring-db-cas-session "0.3.0-SNAPSHOT"
  :description "Clojure-kirjasto Opintoplun CAS-kirjautumiseen ja session tallentamiseen tietokantaan."
  :url "http://example.com/FIXME"
  :license {:name "EUPL, Version 1.1 or - as soon as they will be approved by the European Commission - subsequent versions of the EUPL (the \"Licence\")"
            :url  "http://www.osor.eu/eupl/"}

  :dependencies [[org.clojure/clojure "1.10.1"]
                 [ring "1.8.0"]
                 [metosin/ring-http-response "0.9.1"]
                 [metosin/compojure-api "1.1.13"]
                 [buddy/buddy-auth "2.2.0"]
                 [yesql "0.5.3"]]

  :plugins [[lein-modules "0.3.11"]
            [lein-ancient "0.6.15"]
            [lein-less "1.7.5"]
            [lein-shell "0.5.0"]]

  :min-lein-version "2.5.3")
