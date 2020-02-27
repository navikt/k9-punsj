package no.nav.k9.pleiepengersyktbarn.soknad

import no.nav.k9.kafka.HendelseProducer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


@Service
class PleiepengerSyktBarnSoknadService @Autowired constructor(
        var hendelseProducer: HendelseProducer
) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(PleiepengerSyktBarnSoknadService::class.java)
        const val PLEIEPENGER_SYKT_BARN_TOPIC = "punsjet-soknad-pleiepenger-barn"
    }

    internal suspend fun sendSøknad(
            soeknadJson : String
    ) {

        hendelseProducer.sendTilKafkaTopic(PLEIEPENGER_SYKT_BARN_TOPIC, soeknadJson )
    }


}
