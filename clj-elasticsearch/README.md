# Clj-elasticsearch

Kirjasto, jossa on yhteiskäyttöistä koodia Elasticsearchin HTTP-rajapinnan käyttöön.

## Käyttö

Lisää clj-elasticsearch riippuvuudeksi `project.clj`:ssä:

```
:dependencies [[oph/clj-elasticsearch "0.1.0-SNAPSHOT"]]
```

Ennen kuin kirjastoa voi käyttää, sille pitää konfiguroida Elasticsearchin osoite:

```
(intern 'clj-elasticsearch.elastic-utils 'elastic-host "http://127.0.0.1:9200")
```
