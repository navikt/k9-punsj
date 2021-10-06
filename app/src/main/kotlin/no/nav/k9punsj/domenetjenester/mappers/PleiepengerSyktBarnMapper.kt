package no.nav.k9punsj.domenetjenester.mappers

import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Feil
import no.nav.k9punsj.rest.web.dto.PeriodeDto
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadVisningDto

internal object PleiepengerSyktBarnMapper {

    internal fun mapTilK9Format(
        søknad: PleiepengerSøknadVisningDto,
        soeknadId: String,
        perioderSomFinnesIK9: List<PeriodeDto>,
        journalpostIder: Set<String>): Pair<Søknad, List<Feil>> {

        return MapTilK9FormatV2(
            søknadId = soeknadId,
            journalpostIder = journalpostIder,
            perioderSomFinnesIK9 = perioderSomFinnesIK9,
            dto = søknad
        ).søknadOgFeil()
    }
}