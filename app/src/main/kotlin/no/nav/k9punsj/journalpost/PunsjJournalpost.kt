package no.nav.k9punsj.journalpost

import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

private val logger = LoggerFactory.getLogger(PunsjJournalpost::class.java)

internal data class PunsjJournalpost(
    val uuid: UUID,
    val journalpostId: String,
    val aktørId: String?,
    val skalTilK9: Boolean? = null,
    val mottattDato: LocalDateTime? = null,
    val type: String? = null,
    val ytelse: String? = null,
    val payload: String? = null,
    val fordelStatusType: String? = null,
)

internal fun PunsjJournalpost?.utledeFagsakYtelseType(fagsakYtelseType: FagsakYtelseType? = null): FagsakYtelseType {
    return if (this == null) {
        logger.info("Journalpost er null. Defaulter til ${FagsakYtelseType.PLEIEPENGER_SYKT_BARN.navn}")
        FagsakYtelseType.PLEIEPENGER_SYKT_BARN
    } else {
        when {
            this.ytelse == null -> {
                val type = fagsakYtelseType ?: throw IllegalStateException("Ikke støttet journalpost: $journalpostId, ytelseType: $fagsakYtelseType")
                logger.info("Ytelse på journalpost er null. Utleder fagsakytelsetype fra {} til {}", this.ytelse, type)
                type
            }
            no.nav.k9punsj.db.datamodell.FagsakYtelseType.PLEIEPENGER_SYKT_BARN.kode == this.ytelse -> {
                val type = FagsakYtelseType.PLEIEPENGER_SYKT_BARN
                logger.info("Utleder fagsakytelsetype fra {} til {}", this.ytelse, type)
                type
            }
            no.nav.k9punsj.db.datamodell.FagsakYtelseType.UKJENT.kode == this.ytelse -> {
                val type = fagsakYtelseType ?: throw IllegalStateException("Ikke støttet journalpost: $journalpostId, ytelseType: $fagsakYtelseType")
                logger.info("Utleder fagsakytelsetype fra {} til {}", this.ytelse, type)
                type
            }
            no.nav.k9punsj.db.datamodell.FagsakYtelseType.OMSORGSPENGER.kode == this.ytelse -> {
                val type = FagsakYtelseType.OMSORGSPENGER
                logger.info("Utleder fagsakytelsetype fra {} til {}", this.ytelse, type)
                type
            }
            no.nav.k9punsj.db.datamodell.FagsakYtelseType.OMSORGSPENGER_KRONISK_SYKT_BARN.kode == this.ytelse -> {
                val type = FagsakYtelseType.OMSORGSPENGER_KS
                logger.info("Utleder fagsakytelsetype fra {} til {}", this.ytelse, type)
                type
            }
            no.nav.k9punsj.db.datamodell.FagsakYtelseType.OMSORGSPENGER_MIDLERTIDIG_ALENE.kode == this.ytelse -> {
                val type = FagsakYtelseType.OMSORGSPENGER_MA
                logger.info("Utleder fagsakytelsetype fra {} til {}", this.ytelse, type)
                type
            }
            no.nav.k9punsj.db.datamodell.FagsakYtelseType.PLEIEPENGER_LIVETS_SLUTTFASE.kode == this.ytelse -> {
                val type = FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE
                logger.info("Utleder fagsakytelsetype fra {} til {}", this.ytelse, type)
                type
            }
            else -> {
                throw IllegalStateException("Ikke støttet journalpost: $journalpostId, ytelseType: $fagsakYtelseType")
            }
        }
    }
}
