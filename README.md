# k9-punsj

Håndterer manuell `punching` av papirsøknader som kommer inn for ytelser i Kapittel 9.


## Bygge lokalt
```
export GITHUB_USERNAME=x-access-token
export GITHUB_PASSWORD=et-personal-access-token-med-read-packages-tilgang
./mvnw clean install --settings .m2/settings.xml 
```

Eventuelt om du har satt opp username password for server med id `github` i din lokale settings.xml som håndterer dette.

## Starte lokalt
Bruk klassen `K9PunsjApplicationWithMocks` som en del av `test`

## Henvendelser
 Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.
 
 Interne henvendelser kan sendes via Slack i kanalen #sif_saksbehandling.
