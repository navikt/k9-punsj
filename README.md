# k9-punsj

Håndterer manuell `punching` av papirsøknader som kommer inn for ytelser i Kapittel 9.

[![](https://github.com/navikt/k9-punsj/workflows/Build%20and%20deploy/badge.svg)](https://github.com/navikt/k9-punsj/actions?query=workflow%3A%22Build+and+deploy%22)

## Bygge lokalt
```
export GITHUB_USERNAME=x-access-token
export GITHUB_PASSWORD=et-personal-access-token-med-read-packages-tilgang
./mvnw clean install --settings .m2/settings.xml 
```

Eventuelt om du har satt opp username password for server med id `github` i din lokale settings.xml som håndterer dette.

## Starte lokalt
Bruk klassen `K9PunsjApplicationWithMocks` som en del av `test`


## Swagger lokalt
Bruk header fra Nav token header i authorize
[Swagger](https://localhost:8085/internal/webjars/swagger-ui/index.html?configUrl=/internal/api-docs/swagger-config)

## Accesstoken lokalt
Husk å være logget inn på [localhost](https://localhost:8080) først, så gå til 
[Nav token header](https://localhost:8082/api/k9-punsj/oidc/hentNavTokenHeader)
for å hente token som kan brukes i swagger.

## Swagger i dev
Bruk header fra Nav token header i authorize.
[Swagger](https://k9-punsj.dev.adeo.no/internal/webjars/swagger-ui/index.html?configUrl=/internal/api-docs/swagger-config)

## Accesstoken i dev
Husk å være logget inn på [dev](https://k9-punsj.dev.adeo.no/) først, så gå til 
[Nav token header](https://k9-punsj-oidc-auth-proxy.dev.adeo.no/api/k9-punsj/oidc/hentNavTokenHeader)
for å hente token som kan brukes i swagger.
## Åpne
Link til ulike skjemaer:
[http://localhost:8080](http://localhost:8080)

[Melding om deling av omsorgsdager (Overføring og fordeling av omsorgsdager)](http://localhost:8080/202#/)

[Søknad om ekstra omsorgsdager ved kronisk sykt eller funksjonshemmet barn](http://localhost:8080/203#/)

[Søknad om utbetaling av omsorgspenger når arbeidsgiver ikke utbetaler](http://localhost:8080/204#/)

## Henvendelser
 Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.
 
 Interne henvendelser kan sendes via Slack i kanalen #sif_saksbehandling. 

![k9-punsj](logo.png)
