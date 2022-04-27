package no.nav.k9punsj.felles.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9punsj.objectMapper
import java.time.LocalDate

data class PerioderDto(
    val periodeDto: List<PeriodeDto>
)

data class JournalposterDto(
    val journalposter: MutableSet<String>,
)

internal fun hentUtJournalposter(s: SøknadEntitet): List<String>? = if (s.journalposter != null) {
    val journalposter = objectMapper().convertValue<JournalposterDto>(s.journalposter)
    journalposter.journalposter.toList()
} else null

data class PeriodeDto(
    @JsonFormat(pattern = "yyyy-MM-dd")
    val fom: LocalDate?,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val tom: LocalDate?,
)

data class ArbeidsgiverMedArbeidsforholdId(
    val orgNummerEllerAktørID: String,
    val arbeidsforholdId: List<String>
)

data class SendSøknad(
    val norskIdent: String,
    val soeknadId: String,
)

data class Matchfagsak(
    val brukerIdent: String,
    val barnIdent: String,
)

data class MatchFagsakMedPeriode(
    val brukerIdent: String,
    val periodeDto: PeriodeDto
)

data class OpprettNySøknad(
    val norskIdent: String,
    val journalpostId: String,
    val pleietrengendeIdent: String?,
    val annenPart: String?,
    //TODO endre til å bare bruke pleietrengendeIdent, men støtter både barnIdent og pleietrengendeIdent
    val barnIdent: String?,
)