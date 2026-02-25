# Clj-log

Kirjasto, jossa on yhteiskäyttöistä koodia mm. access- ja error-logitukseen.

## Käyttö

Lisää clj-log riippuvuudeksi `project.clj`:ssä:

```
:dependencies [[oph/clj-log "0.1.0-SNAPSHOT"]]
```

Access-logiin tulevan servicen konfigurointi:

```
(intern 'clj-log.access-log 'service "konfo-backend")
```

