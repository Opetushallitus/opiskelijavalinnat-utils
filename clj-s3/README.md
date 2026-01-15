# Clj-s3

Kirjasto, jossa on yhteiskäyttöistä koodia Amazonin S3:n käyttöön.

## Käyttö

Lisää clj-s3 riippuvuudeksi `project.clj`:ssä:

```
:dependencies [[oph/clj-s3 "0.1.0-SNAPSHOT"]]
```

S3-yhteyden konfigurointi:

```
(intern 'clj-s3.s3-connect 's3-region "eu-west-1")
(intern 'clj-s3.s3-connect 's3-bucket "buketti")
```
