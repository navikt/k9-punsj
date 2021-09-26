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
    internal fun mapTilK9Format(
        søknad: PleiepengerSøknadVisningDto,
        soeknadId: String,
        perioderSomFinnesIK9: List<PeriodeDto>,
        journalpostIder: Set<String>,
        håndterSammenligning: (sammenligningsgrunnlag: Sammenligningsgrunnlag) -> Unit = {
            if (!it.erLike) logger.warn("Mapping ikke like. SøknaderErLike=[${it.søknaderErLike}], FeilErLike=[${it.feilErLike}]")
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
        return sammenligningsgrunnlag.gammel
    }

    internal data class Sammenligningsgrunnlag(
        internal val gammel: Pair<Søknad, List<Feil>>,
        internal val ny: Pair<Søknad, List<Feil>>) {
        internal val gammelSøknadJson = JsonUtils.toString(gammel.first)
        internal val gamleFeilJson = JsonUtils.toString(gammel.second)
        internal val nySøknadJson = JsonUtils.toString(ny.first)
        internal val nyeFeilJson = JsonUtils.toString(ny.second)
        internal val søknaderSammenlignet = JSONCompare.compareJSON(gammelSøknadJson, nySøknadJson, JSONCompareMode.NON_EXTENSIBLE)
        internal val feilSammenlignet = JSONCompare.compareJSON(gamleFeilJson, nyeFeilJson, JSONCompareMode.NON_EXTENSIBLE)
        internal val søknaderErLike = søknaderSammenlignet.passed()
        internal val feilErLike = feilSammenlignet.passed()
        internal val erLike = søknaderErLike && feilErLike
    }

    private val logger = LoggerFactory.getLogger(PleiepengerSyktBarnMapper::class.java)
}