package no.nav.k9punsj.arbeidsgivere

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
internal class ArbeidsgiverService(
    @Value("\${ENABLE_AAREG}") private val enableAareg: Boolean = false,
    private val aaregClient: AaregClient) {

    internal suspend fun hentArbeidsgivere(
        identitetsnummer: String,
        fom: LocalDate,
        tom: LocalDate) : Set<Arbeidsgiver> {
        return when (enableAareg) {
            true -> aaregClient.hentArbeidsgivere(
                identitetsnummer = identitetsnummer,
                fom = fom,
                tom = tom
            )
            else -> emptySet()
        }
    }
}