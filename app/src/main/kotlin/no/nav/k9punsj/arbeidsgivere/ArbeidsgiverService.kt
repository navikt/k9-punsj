package no.nav.k9punsj.arbeidsgivere

import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
internal class ArbeidsgiverService() {
    internal suspend fun hentArbeidsgivere(
        identitetsnummer: String,
        fom: LocalDate,
        tom: LocalDate) : Set<Arbeidsgiver> {
        return when (identitetsnummer) {
            "11111111111" -> setOf(
                Arbeidsgiver(
                    organisasjonsnummer = "979312059",
                    navn = "NAV AS"
                )
            )
            else -> emptySet()
        }
    }
}