package no.nav.k9punsj.domenetjenester.mappers

import no.nav.k9.søknad.JsonUtils
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Feil
import no.nav.k9punsj.rest.web.dto.PeriodeDto
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadVisningDto
import org.skyscreamer.jsonassert.JSONCompare
import org.skyscreamer.jsonassert.JSONCompareMode
import org.slf4j.LoggerFactory

internal object PleiepengerSyktBarnMapper {
    private val logger = LoggerFactory.getLogger(PleiepengerSyktBarnMapper::class.java)
    private val isProd = System.getenv("NAIS_CLUSTER_NAME").let { it != null && it.lowercase().startsWith("prod") }

    internal fun mapTilK9Format(
        søknad: PleiepengerSøknadVisningDto,
        soeknadId: String,
        perioderSomFinnesIK9: List<PeriodeDto>,
        journalpostIder: Set<String>,
        håndterSammenligning: (sammenligningsgrunnlag: Sammenligningsgrunnlag) -> Unit = {
            if (it.forskjelligeFeil) {
                logger.warn("Mapping ga forskjellige feil. GamleFeil=${it.gamleFeilJson}, NyeFeil=${it.nyeFeilJson}]")
            }
            if (it.forskjelligeSøknader) {
                logger.warn("Mapping ga forskjellige søknader. Felt=${it.søknaderSammenlignet.fieldFailures.map { failure -> failure.field }}")
                if (!isProd) {
                    logger.warn("Søknadsforskjeller=[${it.søknaderSammenlignet.message}]")
                }
            }
        }): Pair<Søknad, List<Feil>> {
        val sammenligningsgrunnlag = Sammenligningsgrunnlag(
            gammel = MapTilK9Format.mapTilEksternFormat(
                søknad = MapFraVisningTilEksternFormat.mapTilSendingsformat(søknad),
                soeknadId = soeknadId,
                perioderSomFinnesIK9 = perioderSomFinnesIK9,
                journalpostIder = journalpostIder
            ),
            ny = MapTilK9FormatV2(
                søknadId = soeknadId,
                journalpostIder = journalpostIder,
                perioderSomFinnesIK9 = perioderSomFinnesIK9,
                dto = søknad
            ).søknadOgFeil()
        )
        håndterSammenligning(sammenligningsgrunnlag)
        return sammenligningsgrunnlag.ny
    }

    internal data class Sammenligningsgrunnlag(
        internal val gammel: Pair<Søknad, List<Feil>>,
        internal val ny: Pair<Søknad, List<Feil>>) {
        private val gammelSøknadJson = gammel.first.toJson()
        internal val gamleFeilJson = gammel.second.map { it.toMap() }.toJson()
        private val nySøknadJson = ny.first.toJson()
        internal val nyeFeilJson = ny.second.map { it.toMap() }.toJson()
        internal val søknaderSammenlignet = JSONCompare.compareJSON(gammelSøknadJson, nySøknadJson, JSONCompareMode.NON_EXTENSIBLE)
        private val feilSammenlignet = JSONCompare.compareJSON(gamleFeilJson, nyeFeilJson, JSONCompareMode.NON_EXTENSIBLE)
        internal val forskjelligeSøknader = søknaderSammenlignet.failed()
        internal val forskjelligeFeil = feilSammenlignet.failed()
        private fun Any.toJson() = JsonUtils.getObjectMapper().writeValueAsString(this)
        private fun Feil.toMap() = mapOf(
            "felt" to felt,
            "feilkode" to feilkode,
            "feilmelding" to feilmelding
        )
    }
}