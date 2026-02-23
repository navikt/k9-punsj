# Migrering til ProblemDetail

## mål

Alle feilresponser skal bruke `ProblemDetail` som felles kontrakt i stedet for en blanding av `OasFeil`, `SøknadFeil` og plain text.

## hvorfor

- Frontend får én forutsigbar feilmodell for alle endepunkter.
- Feil blir enklere å logge, spore og vise med samme felter (`type`, `title`, `status`, `detail`, `correlationId`).
- OpenAPI blir i samsvar med faktisk runtime oppførsel.
- Risiko for parse feil i klient reduseres.

## omfang backend

Følgende områder må migreres:

- Tjenester med `send` eller `valider` som fortsatt returnerer `OasFeil` eller tekst.
- Ruter som bruker `bodyValueAndAwait(e.localizedMessage)` eller lignende.
- OpenAPI filer som fortsatt refererer `OasFeil` eller andre legacy feilmodeller.

Prioritert rekkefølge:

1. Fullføre migrering i `PleiepengerSyktBarnService` først (`send`, deretter `valider`).
2. `send` endepunkter for PLS, OMPMA, OMPKS, OMPUT, OLP og KIM.
3. `valider` endepunkter for samme domener.
4. Øvrige ruter med `OasFeil` utenfor søknadsflyt.

## omfang frontend

Frontend må oppdateres i `k9-punsj-frontend` slik at feilhåndtering følger samme modell:

- `submit` og `valider` actions skal lese valideringsfeil fra `ProblemDetail` og sende øvrige feil til generell feilhåndtering.
- Delte hjelpefunksjoner for mapping av `ProblemDetail` skal brukes konsekvent.
- Konflikt og generell innsendingsfeil skal vises fra normalisert `detail` (evt. `title` som fallback).
- Legacy avhengighet til `responseData.feil` og tekstformat i `detail` skal fjernes.

## føringer for migrering

- Bruk `detail` som primært tekstfelt i frontend.
- Unngå nye kompatibilitetsfelter som dupliserer `detail`.
- Bruk `ErrorResponseException` for feil som skal serialiseres globalt.
- La global feilserialisering i `CoroutineRequestContext` være felles utgangspunkt.
- `CoroutineRequestContext` logger i dag også 4xx som `ERROR`. Dette beholdes foreløpig og håndteres i en egen oppgave.
- Ikke bland nye og gamle kontrakter i samme endepunkt.

## ferdig når

- Alle berørte endepunkter dokumenterer `ProblemDetail` i OpenAPI.
- Runtime respons for feil er JSON basert `ProblemDetail` for de samme endepunktene.
- Frontend håndterer alle disse feilene uten særregler per domene.
