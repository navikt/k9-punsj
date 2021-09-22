package no.nav.k9punsj.arbeidsgivere

internal data class Arbeidsgiver(
    val organisasjonsnummer: String,
    val navn: String
)

internal data class ArbeidsgivereResponse(
    val arbeidsgivere: Set<Arbeidsgiver>
)