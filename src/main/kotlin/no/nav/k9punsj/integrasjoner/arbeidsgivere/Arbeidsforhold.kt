package no.nav.k9punsj.integrasjoner.arbeidsgivere

internal data class Arbeidsforhold(
    val organisasjoner: Set<OrganisasjonArbeidsforhold>
)

internal data class OrganisasjonArbeidsforhold(
    val arbeidsforholdId: String?,
    val organisasjonsnummer: String
)
