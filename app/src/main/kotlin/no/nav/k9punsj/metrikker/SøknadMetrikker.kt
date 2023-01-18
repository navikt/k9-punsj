package no.nav.k9punsj.metrikker

import io.micrometer.core.instrument.MeterRegistry
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.ytelse.Ytelse
import no.nav.k9.søknad.ytelse.olp.v1.Opplæringspenger
import no.nav.k9.søknad.ytelse.omsorgspenger.utvidetrett.v1.OmsorgspengerAleneOmsorg
import no.nav.k9.søknad.ytelse.omsorgspenger.utvidetrett.v1.OmsorgspengerKroniskSyktBarn
import no.nav.k9.søknad.ytelse.omsorgspenger.utvidetrett.v1.OmsorgspengerMidlertidigAlene
import no.nav.k9.søknad.ytelse.omsorgspenger.v1.OmsorgspengerUtbetaling
import no.nav.k9.søknad.ytelse.pls.v1.PleipengerLivetsSluttfase
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

private val logger = LoggerFactory.getLogger("no.nav.k9punsj.metrikker.SøknadMetrikkerKt.publiserMetrikker")

@Suppress("MoveVariableDeclarationIntoWhen")
@Service
internal class SøknadMetrikkService(
    private val ytelseMetrikker: YtelseMetrikker
) {
    fun publiserMetrikker(søknad: Søknad) {
        try {
            // må deklares her for at smart case skal funke
            val ytelse = søknad.getYtelse<Ytelse>()
            ytelseMetrikker.publiserMetrikker(ytelse, søknad)
        } catch (e: Exception) {
            logger.warn("Feilet med publisering av metrikker", e)
        }
    }
}
