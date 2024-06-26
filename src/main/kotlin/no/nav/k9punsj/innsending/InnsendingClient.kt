package no.nav.k9punsj.innsending

import com.fasterxml.jackson.module.kotlin.convertValue
import de.huxhorn.sulky.ulid.ULID
import kotlinx.coroutines.runBlocking
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.k9.søknad.Søknad
import no.nav.k9punsj.journalpost.dto.KopierJournalpostInfo
import no.nav.k9punsj.utils.objectMapper
import org.slf4j.LoggerFactory
import java.util.*

interface InnsendingClient {
    fun mapKopierJournalpost(info: KopierJournalpostInfo): Pair<String, String> {
        val behovssekvensId = ulid.nextULID()
        val correlationId = UUID.randomUUID().toString()
        logger.info(
            "Sender journalpost (${info.journalpostId}) til kopiering med ytelse ${info.ytelse.kode}",
            keyValue("journalpost_id", info.journalpostId),
            keyValue("correlation_id", correlationId),
            keyValue("behovssekvens_id", behovssekvensId)
        )
        return Behovssekvens(
            id = behovssekvensId,
            correlationId = correlationId,
            behov = arrayOf(
                Behov(
                    navn = KopierPunsjbarJournalpostBehovNavn,
                    input = mapOf(
                        VersjonKey to KopierPunsjbarJournalpostVersjon,
                        "journalpostId" to info.journalpostId,
                        "fra" to info.fra,
                        "til" to info.til,
                        "pleietrengende" to info.pleietrengende,
                        "annenPart" to info.annenPart,
                        "søknadstype" to info.ytelse.somSøknadstype()
                    )
                )
            )
        ).keyValue
    }

    suspend fun sendKopierJournalpost(info: KopierJournalpostInfo) = send(mapKopierJournalpost(info))

    suspend fun send(pair: Pair<String, String>)

    companion object {
        private val logger = LoggerFactory.getLogger(InnsendingClient::class.java)
        private val objectMapper = objectMapper()
        private val ulid = ULID()
        private const val VersjonKey = "versjon"

        private const val KopierPunsjbarJournalpostBehovNavn = "KopierPunsjbarJournalpost"
        private const val KopierPunsjbarJournalpostVersjon = "1.0.0"

        private fun FagsakYtelseType.somSøknadstype() = when (this) {
            FagsakYtelseType.PLEIEPENGER_SYKT_BARN -> "PleiepengerSyktBarn"
            FagsakYtelseType.OMSORGSPENGER -> "Omsorgspenger"
            FagsakYtelseType.OMSORGSPENGER_KS -> "OmsorgspengerKroniskSyktBarn"
            FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE -> "PleiepengerLivetsSluttfase"
            else -> throw IllegalArgumentException("Støtter ikke ytelse ${this.navn}")
        }

        internal fun Søknad.somMap(): Map<String, *> = objectMapper.convertValue(this)
    }
}
