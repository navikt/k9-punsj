package no.nav.k9punsj.felles.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9punsj.utils.objectMapper
import java.time.LocalDate

data class PerioderDto(
    val periodeDto: List<PeriodeDto>
)

data class JournalposterDto(
    val journalposter: MutableSet<String>
)

internal fun hentUtJournalposter(s: SøknadEntitet): List<String>? = if (s.journalposter != null) {
    val journalposter = objectMapper().convertValue<JournalposterDto>(s.journalposter)
    journalposter.journalposter.toList()
} else null

data class PeriodeDto(
    @JsonFormat(pattern = "yyyy-MM-dd")
    val fom: LocalDate?,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val tom: LocalDate?
)

data class ArbeidsgiverMedArbeidsforholdId(
    val orgNummerEllerAktørID: String,
    val arbeidsforholdId: List<String>
)

data class SendSøknad(
    val norskIdent: String,
    val soeknadId: String
)

data class Matchfagsak(
    val brukerIdent: String,
    val barnIdent: String? = null,
    val periode: PeriodeDto? = null
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
)

data class PleietrengendeDto(
    val norskIdent: String? = null,
    val foedselsdato: LocalDate? = null
)

data class BostederDto(
    val periode: PeriodeDto?,
    val land: String?
)

data class UtenlandsoppholdDto(
    val periode: PeriodeDto?,
    val land: String?,
    val årsak: String?
)

data class UtenlandsoppholdDtoV2(
    val periode: PeriodeDto? = null,
    val land: String? = null,
    val innleggelsesperioder: List<InnleggelsesperiodeDto> = emptyList()
) {
    data class InnleggelsesperiodeDto(
        val årsak: String?,
        val periode: PeriodeDto?
    )
}
