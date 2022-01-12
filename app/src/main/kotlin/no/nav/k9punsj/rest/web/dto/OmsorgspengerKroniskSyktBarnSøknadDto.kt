package no.nav.k9punsj.rest.web.dto

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.k9punsj.domenetjenester.mappers.DurationMapper.somDuration
import no.nav.k9punsj.domenetjenester.mappers.DurationMapper.somTimerOgMinutter
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadDto.TimerOgMinutter.Companion.somTimerOgMinutterDto
import java.time.LocalDate
import java.time.LocalTime


data class OmsorgspengerKroniskSyktBarnSøknadDto(
    val soeknadId: SøknadIdDto,
    val soekerId: NorskIdentDto? = null,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val mottattDato: LocalDate? = null,
    @JsonFormat(pattern = "HH:mm")
    val klokkeslett : LocalTime? = null,
    val barn: BarnDto? = null,
    val kroniskEllerFunksjonshemming: Boolean? = null,
    val journalposter: List<JournalpostIdDto>? = null,
) {
    data class BarnDto(
        val norskIdent: NorskIdentDto?,
        @JsonFormat(pattern = "yyyy-MM-dd")
        val foedselsdato: LocalDate?
    )
}
