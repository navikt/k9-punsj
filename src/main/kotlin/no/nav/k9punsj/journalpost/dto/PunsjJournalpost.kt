package no.nav.k9punsj.journalpost.dto

import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9punsj.fordel.K9FordelType
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

private val logger = LoggerFactory.getLogger(PunsjJournalpost::class.java)

data class PunsjJournalpost(
    val uuid: UUID,
    val journalpostId: String,
    val aktørId: String?,
    val skalTilK9: Boolean? = null,
    val mottattDato: LocalDateTime? = null,
    val type: String? = K9FordelType.UKJENT.kode,
    val gosysoppgaveId: String? = null,
    val ytelse: String? = null,
    val payload: String? = null,
    val behandlingsAar: Int? = null,
    val fordelStatusType: String? = null
)

internal fun PunsjJournalpost?.utledK9sakFagsakYtelseType(k9sakFagsakYtelseType: FagsakYtelseType): FagsakYtelseType {
    return if (this == null) {
        logger.info("Journalpost er null. Defaulter til ${FagsakYtelseType.PLEIEPENGER_SYKT_BARN.navn}")
        FagsakYtelseType.PLEIEPENGER_SYKT_BARN
    } else {
        val ytelse = when (this.ytelse) {
            null -> {
                logger.info("Journalpost har ingen ytelse. Bruker ${k9sakFagsakYtelseType.kode}")
                k9sakFagsakYtelseType
            }
            no.nav.k9punsj.felles.FagsakYtelseType.UKJENT.kode -> FagsakYtelseType.UDEFINERT
            no.nav.k9punsj.felles.FagsakYtelseType.PLEIEPENGER_SYKT_BARN.kode -> FagsakYtelseType.PLEIEPENGER_SYKT_BARN
            no.nav.k9punsj.felles.FagsakYtelseType.OMSORGSPENGER.kode -> FagsakYtelseType.OMSORGSPENGER
            no.nav.k9punsj.felles.FagsakYtelseType.OMSORGSPENGER_KRONISK_SYKT_BARN.kode -> FagsakYtelseType.OMSORGSPENGER_KS
            no.nav.k9punsj.felles.FagsakYtelseType.OMSORGSPENGER_MIDLERTIDIG_ALENE.kode -> FagsakYtelseType.OMSORGSPENGER_MA
            no.nav.k9punsj.felles.FagsakYtelseType.OMSORGSPENGER_ALENE_OMSORGEN.kode -> FagsakYtelseType.OMSORGSPENGER_AO
            no.nav.k9punsj.felles.FagsakYtelseType.PLEIEPENGER_LIVETS_SLUTTFASE.kode -> FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE
            no.nav.k9punsj.felles.FagsakYtelseType.OPPLÆRINGSPENGER.kode -> FagsakYtelseType.OPPLÆRINGSPENGER
            else -> throw IllegalStateException("Ikke støttet journalpost: $journalpostId, ytelseType: $k9sakFagsakYtelseType")
        }
        logger.info("Utleder fagsakytelsetype fra {} til {}", this.ytelse, ytelse)
        ytelse
    }
}
