package no.nav.k9punsj.arbeidsgivere

import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId

@Service
internal class ArbeidsgiverService() {
    internal suspend fun hentArbeidsgivere(
        identitetsnummer: String,
        fom: LocalDate = LocalDate.now(Oslo).minusMonths(6),
        tom: LocalDate = LocalDate.now(Oslo).plusMonths(6)) : Set<Arbeidsgiver> {
        return setOf(
            Arbeidsgiver(
                organisasjonsnummer = "987676789",
                navn = "Kiwi AS"
        ))
    }

    private companion object  {
        private val Oslo = ZoneId.of("Europe/Oslo")
    }
}