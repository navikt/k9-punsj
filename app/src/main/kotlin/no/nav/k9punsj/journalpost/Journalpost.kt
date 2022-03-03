package no.nav.k9punsj.journalpost

import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9punsj.db.datamodell.AktørId
import no.nav.k9punsj.rest.web.JournalpostId
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

private val logger = LoggerFactory.getLogger(Journalpost::class.java)

data class Journalpost(
    val uuid: UUID,
    val journalpostId: JournalpostId,
    val aktørId: AktørId?,
    val skalTilK9: Boolean? = null,
    val mottattDato: LocalDateTime? = null,
    val type: String? = null,
    val ytelse: String? = null,
    val payload: String? = null,
    val fordelStatusType: String? = null,
    val opprinneligJournalpost: OpprinneligJournalpost? = null,
) {
    data class OpprinneligJournalpost(
        val journalpostId: JournalpostId?,
    )
}

fun Journalpost?.utledeFagsakYtelseType(): FagsakYtelseType {
    return if (this == null) FagsakYtelseType.PLEIEPENGER_SYKT_BARN
    else {
        when {
            this.ytelse != null && no.nav.k9punsj.db.datamodell.FagsakYtelseType.OMSORGSPENGER.kode == this.ytelse -> {
                val type = FagsakYtelseType.OMSORGSPENGER
                logger.info("Utleder fagsakytelsetype fra {} til {}", this.ytelse, type)
                type
            }
            this.ytelse != null && no.nav.k9punsj.db.datamodell.FagsakYtelseType.PLEIEPENGER_SYKT_BARN.kode == this.ytelse -> {
                val type = FagsakYtelseType.PLEIEPENGER_SYKT_BARN
                logger.info("Utleder fagsakytelsetype fra {} til {}", this.ytelse, type)
                type
            }
            this.ytelse != null && no.nav.k9punsj.db.datamodell.FagsakYtelseType.OMSORGSPENGER_KRONISK_SYKT_BARN.kode == this.ytelse -> {
                val type = FagsakYtelseType.OMSORGSPENGER_KS
                logger.info("Utleder fagsakytelsetype fra {} til {}", this.ytelse, type)
                type
            }
            else -> {
                throw IllegalStateException("Ikke støttet journalpost: $this")
            }
        }
    }
}
