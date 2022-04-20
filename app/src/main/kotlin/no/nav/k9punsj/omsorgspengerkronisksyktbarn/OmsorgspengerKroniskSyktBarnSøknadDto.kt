package no.nav.k9punsj.omsorgspengerkronisksyktbarn

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9punsj.db.datamodell.FagsakYtelseType
import no.nav.k9punsj.db.datamodell.Mappe
import no.nav.k9punsj.db.datamodell.SøknadEntitet
import no.nav.k9punsj.domenetjenester.dto.*
import no.nav.k9punsj.domenetjenester.dto.hentUtJournalposter
import no.nav.k9punsj.objectMapper
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
    val journalposter: List<JournalpostIdDto>? = null,
    val harInfoSomIkkeKanPunsjes : Boolean,
    val harMedisinskeOpplysninger : Boolean
) {
    data class BarnDto(
        val norskIdent: NorskIdentDto?,
        @JsonFormat(pattern = "yyyy-MM-dd")
        val foedselsdato: LocalDate?
    )
}

data class SvarOmsKSBDto(
    val søker: NorskIdentDto,
    val fagsakTypeKode: String,
    val søknader: List<OmsorgspengerKroniskSyktBarnSøknadDto>?,
)

internal fun Mappe.tilOmsKSBVisning(norskIdent: NorskIdentDto): SvarOmsKSBDto {
    val bunke = hentFor(FagsakYtelseType.OMSORGSPENGER_KRONISK_SYKT_BARN)
    if (bunke?.søknader.isNullOrEmpty()) {
        return SvarOmsKSBDto(norskIdent, FagsakYtelseType.OMSORGSPENGER_KRONISK_SYKT_BARN.kode, listOf())
    }
    val søknader = bunke?.søknader
        ?.filter { s -> !s.sendtInn }
        ?.map { s ->
            if (s.søknad != null) {
                objectMapper().convertValue(s.søknad)
            } else {
                OmsorgspengerKroniskSyktBarnSøknadDto(
                    soeknadId = s.søknadId,
                    journalposter = hentUtJournalposter(s),
                    harMedisinskeOpplysninger = false,
                    harInfoSomIkkeKanPunsjes = false
                )
            }
        }
    return SvarOmsKSBDto(norskIdent, FagsakYtelseType.OMSORGSPENGER_KRONISK_SYKT_BARN.kode, søknader)
}

internal fun SøknadEntitet.tilOmsKSBvisning(): OmsorgspengerKroniskSyktBarnSøknadDto {
    if (søknad == null) {
        return OmsorgspengerKroniskSyktBarnSøknadDto(
            soeknadId = this.søknadId,
            harMedisinskeOpplysninger = false,
            harInfoSomIkkeKanPunsjes = false
        )
    }
    return objectMapper().convertValue(søknad)
}
