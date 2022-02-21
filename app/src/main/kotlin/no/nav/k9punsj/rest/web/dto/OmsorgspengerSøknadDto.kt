package no.nav.k9punsj.rest.web.dto

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.k9punsj.domenetjenester.mappers.DurationMapper.somDuration
import no.nav.k9punsj.domenetjenester.mappers.DurationMapper.somTimerOgMinutter
import no.nav.k9punsj.rest.web.dto.PleiepengerSyktBarnSøknadDto.TimerOgMinutter.Companion.somTimerOgMinutterDto
import java.time.LocalDate
import java.time.LocalTime


data class OmsorgspengerSøknadDto(
    val soeknadId: SøknadIdDto,
    val soekerId: NorskIdentDto? = null,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val mottattDato: LocalDate? = null,
    @JsonFormat(pattern = "HH:mm")
    val klokkeslett : LocalTime? = null,
    val journalposter: List<JournalpostIdDto>? = null,
    val organisasjonsnummer: String? = null,
    val arbeidsforholdId: String? = null,
    val fravaersperioder: List<FraværPeriode>? = null,
    val harInfoSomIkkeKanPunsjes : Boolean? = null,
    val harMedisinskeOpplysninger : Boolean? = null
) {
    data class FraværPeriode(
        val periode: PeriodeDto,
        val faktiskTidPrDag: String?,
        val tidPrDag: PleiepengerSyktBarnSøknadDto.TimerOgMinutter? = faktiskTidPrDag?.somDuration()?.somTimerOgMinutter()?.somTimerOgMinutterDto(),
    )
}
