package no.nav.k9punsj.integrasjoner.arbeidsgivere

internal data class ArbeidsgivereMedArbeidsforholdId(
    val organisasjoner: Set<OrganisasjonArbeidsgiverMedId>
)

internal data class OrganisasjonArbeidsgiverMedId(
    val organisasjonsnummer: String,
    val navn: String,
    val arbeidsforhold: List<String>
)
