# Progress

## 2026-02-12

- Implementerte PSB `valider` runtime kontrakt med `ProblemDetail` for `400` og `500`, inkludert `soeknadId` og `feil` i
  properties for feltmapping.
- Oppdaterte OpenAPI for PSB `valider` til `application/problem+json` med `ProblemDetail` for `400` og `500`.
- La til dedikerte kontraktstester for PSB `valider` for `400` valideringsfeil, `400` manglende søknad, `500` uventet
  feil og `202` suksess.
- Verifiserte målrettet backend kontraktstestkjøring `PleiepengerSyktBarnValiderErrorContractTest` (`5` tester bestått).

## 2026-02-10

- Standardiserte `POST /api/pleiepenger-sykt-barn-soknad/send` på `ProblemDetail` for HTTP 400, 409 og 500.
- Fjernet `feilmelding` som kompatibilitetsfelt for PSB send, frontend bruker nå `detail` og `title`.
- Beholdt valideringsfeil i `properties.feil` for HTTP 400.
- Oppdaterte OpenAPI for PSB send til `application/problem+json` og `ProblemDetail` for 400, 409 og 500.
- Sluttet å pakke `RestKallException` om til plain text i K9 Sak send flyt.
- La til og oppdaterte tester for PSB send kontrakt, inkludert scenarioer for JSON og ikke JSON upstream feil.
