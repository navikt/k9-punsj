package no.nav.k9punsj.rest.web.dto

import no.nav.k9punsj.domenetjenester.mappers.DurationMapper.somDuration
import no.nav.k9punsj.domenetjenester.mappers.DurationMapper.somTimerOgMinutter
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadDto.TimerOgMinutter.Companion.somTimerOgMinutterDto


data class OmsorgspengerSøknadDto(
    val soeknadId: SøknadIdDto,
    val soekerId: NorskIdentDto? = null,
    val journalposter: List<JournalpostIdDto>? = null,
    val organisasjonsnummer: String? = null,
    val arbeidsforholdId: String? = null,
    val fravaersperioder: List<FraværPeriode>? = null,
) {
    data class FraværPeriode(
        val periode: PeriodeDto,
        val faktiskTidPrDag: String?,
        val tidPrDag: PleiepengerSøknadDto.TimerOgMinutter? = faktiskTidPrDag?.somDuration()?.somTimerOgMinutter()?.somTimerOgMinutterDto(),
    )
}
