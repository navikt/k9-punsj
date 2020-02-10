# k9-punsj

Håndterer manuell `punching` av papirsøknader som kommer inn for ytelser i Kapittel 9.

[![](https://github.com/navikt/k9-punsj/workflows/Build%20and%20deploy/badge.svg)](https://github.com/navikt/k9-punsj/actions?query=workflow%3A%22Build+and+deploy%22)

## Bygge lokalt
```
export GITHUB_USERNAME=x-access-token
export GITHUB_PASSWORD=af06c6f24bfafff3c4062f404f772eb770f4ad12
./mvnw clean install --settings .m2/settings.xml 
```

Eventuelt om du har satt opp username password for server med id `github` i din lokale settings.xml som håndterer dette.

## Starte lokalt
Bruk klassen `K9PunsjApplicationWithMocks` som en del av `test`

## Henvendelser
 Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.
 
 Interne henvendelser kan sendes via Slack i kanalen #sif_saksbehandling.

![k9-punsj](logo.png)
