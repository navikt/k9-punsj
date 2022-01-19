package no.nav.k9punsj.arbeidsgivere

internal data class Arbeidsforhold (
    val organisasjoner: Set<OrganisasjonArbeidsforhold>,
)

internal data class OrganisasjonArbeidsforhold(
    val arbeidsforholdId: String?,
    val organisasjonsnummer: String
)
