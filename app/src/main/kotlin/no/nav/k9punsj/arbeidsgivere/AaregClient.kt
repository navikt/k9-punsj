package no.nav.k9punsj.arbeidsgivere

import java.time.LocalDate

internal interface AaregClient {
    suspend fun hentArbeidsforhold(
        identitetsnummer: String,
        fom: LocalDate,
        tom: LocalDate
    ) : Arbeidsforhold
}