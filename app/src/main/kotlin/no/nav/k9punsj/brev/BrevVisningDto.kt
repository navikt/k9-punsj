package no.nav.k9punsj.brev

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.k9punsj.rest.web.dto.JournalpostIdDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class BrevVisningDto(
    @JsonFormat(pattern = "yyyy-MM-dd")
    val mottattDato: LocalDate,
    @JsonFormat(pattern = "HH:mm")
    val klokkeslett: LocalTime,
    val journalpostId: JournalpostIdDto,
    val mottaker: DokumentbestillingDto.Mottaker,
    val saksnummer: String,
    val sendtInnAv: String,
) {

    internal companion object {
        private fun LocalDateTime.splittIDagOgKlokkeslett() : Pair<LocalDate, LocalTime> {
            val localDate: LocalDate = this.toLocalDate()
            val localTime: LocalTime = this.toLocalTime()
            return localDate to localTime
        }

        internal fun lagBrevVisningDto(dto: DokumentbestillingDto, brevEntitet: BrevEntitet) : BrevVisningDto {
            brevEntitet.opprettet_tid
            val (mottattDato, klokkeslett) = brevEntitet.opprettet_tid!!.splittIDagOgKlokkeslett()
            return BrevVisningDto(
                mottattDato = mottattDato,
                klokkeslett = klokkeslett,
                journalpostId = dto.journalpostId,
                mottaker = dto.mottaker,
                saksnummer = dto.saksnummer,
                sendtInnAv = brevEntitet.opprettet_av
            )
        }
    }
}

