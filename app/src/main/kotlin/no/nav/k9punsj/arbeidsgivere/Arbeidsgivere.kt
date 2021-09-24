package no.nav.k9punsj.arbeidsgivere

internal data class Arbeidsgivere(
    val organisasjoner: Set<OrganisasjonArbeidsgiver>
)

internal data class OrganisasjonArbeidsgiver(
    val organisasjonsnummer: String,
    val navn: String
)
