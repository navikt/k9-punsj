package no.nav.k9punsj.arbeidsgivere

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.LocalDate

@Profile("test | local")
@Component
internal class LocalAaregClient : AaregClient {
    override suspend fun hentArbeidsforhold(identitetsnummer: String, fom: LocalDate, tom: LocalDate) = when {
        identitetsnummer == "22222222222" -> Arbeidsforhold(organisasjoner = emptySet())
        else -> Arbeidsforhold(organisasjoner = setOf(
            OrganisasjonArbeidsforhold(
                arbeidsforholdId = "1",
                organisasjonsnummer = "979312059"
        )))
    }
}