package no.nav.k9punsj.innsending

import com.fasterxml.jackson.module.kotlin.convertValue
import de.huxhorn.sulky.ulid.ULID
import net.logstash.logback.argument.StructuredArguments.keyValue
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
                navn = BehovNavn,
                input = tilleggsOpplysninger
                    .plus(SøknadKey to søknad)
                    .plus(VersjonKey to Versjon)
            ))
        ).keyValue
    }

    fun sendSøknad(søknadId: String, søknad: Any, correlationId: CorrelationId, tilleggsOpplysninger: Map<String, Any> = emptyMap()) {
        send(mapSøknad(søknadId, søknad, correlationId, tilleggsOpplysninger))
    }

    fun send(pair: Pair<String, String>)

    companion object {
        private val logger = LoggerFactory.getLogger(InnsendingClient::class.java)
        private val objectMapper = objectMapper()
        private val ulid = ULID()
        private const val BehovNavn = "PunsjetSøknad"
        private const val SøknadKey = "søknad"
        private const val VersjonKey = "versjon"
        private const val Versjon = "1.0.0"
    }
}