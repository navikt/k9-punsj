package no.nav.k9punsj.innsending

import com.fasterxml.jackson.module.kotlin.convertValue
import de.huxhorn.sulky.ulid.ULID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.k9punsj.CorrelationId
import no.nav.k9punsj.objectMapper
import org.slf4j.LoggerFactory

interface InnsendingClient {
    fun mapSøknad(søknadId: String, søknad: Any, correlationId: CorrelationId, tilleggsOpplysninger: Map<String, Any>) : Pair<String, String> {
        val søknad: Map<String, *> = objectMapper.convertValue(søknad)
        val behovssekvensId = ulid.nextULID()

        logger.info("Sender søknad. Tilleggsopplysninger=${tilleggsOpplysninger.keys}",
            keyValue("soknad_id", søknadId),
            keyValue("correlation_id", correlationId),
            keyValue("behovssekvens_id", behovssekvensId)
        )
        return Behovssekvens(
            id = behovssekvensId,
            correlationId = correlationId,
            behov = arrayOf(Behov(
                navn = PunsjetSøknadBehovNavn,
                input = tilleggsOpplysninger
                    .plus(PunsjetSøknadSøknadKey to søknad)
                    .plus(VersjonKey to PunsjetSøknadVersjon)
            ))
        ).keyValue
    }

    fun sendSøknad(søknadId: String, søknad: Any, correlationId: CorrelationId, tilleggsOpplysninger: Map<String, Any> = emptyMap()) {
        send(mapSøknad(søknadId, søknad, correlationId, tilleggsOpplysninger))
    }

    fun mapKopierJournalpost(info: KopierJournalpostInfo) : Pair<String, String> {
        val behovssekvensId = "${info.id}"
        logger.info("Sender journalpost til kopiering.",
            keyValue("journalpost_id", info.journalpostId),
            keyValue("correlation_id", info.correlationId),
            keyValue("behovssekvens_id", behovssekvensId)
        )
        return Behovssekvens(
            id = behovssekvensId,
            correlationId = info.correlationId,
            behov = arrayOf(Behov(
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
            ))
        ).keyValue
    }

    fun sendKopierJournalpost(info: KopierJournalpostInfo) = send(mapKopierJournalpost(info))

    fun send(pair: Pair<String, String>)

    companion object {
        private val logger = LoggerFactory.getLogger(InnsendingClient::class.java)
        private val objectMapper = objectMapper()
        private val ulid = ULID()
        private const val VersjonKey = "versjon"

        private const val PunsjetSøknadBehovNavn = "PunsjetSøknad"
        private const val PunsjetSøknadSøknadKey = "søknad"
        private const val PunsjetSøknadVersjon = "1.0.0"

        private const val KopierPunsjbarJournalpostBehovNavn = "KopierPunsjbarJournalpost"
        private const val KopierPunsjbarJournalpostVersjon = "1.0.0"

        private fun FagsakYtelseType.somSøknadstype() = when (this) {
            FagsakYtelseType.PLEIEPENGER_SYKT_BARN -> "PleiepengerSyktBarn"
            else -> throw IllegalArgumentException("Støtter ikke ytelse ${this.navn}")
        }
    }
}