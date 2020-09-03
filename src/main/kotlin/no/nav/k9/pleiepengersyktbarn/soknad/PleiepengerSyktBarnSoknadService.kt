package no.nav.k9.pleiepengersyktbarn.soknad

import no.nav.k9.JournalpostId
import no.nav.k9.kafka.HendelseProducer
import no.nav.k9.søknad.pleiepengerbarn.PleiepengerBarnSøknad
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
        const val PLEIEPENGER_SYKT_BARN_TOPIC = "privat-punsjet-pleiepengesoknad"
    }

    internal suspend fun sendSøknad(søknad: PleiepengerBarnSøknad, journalpostIder: MutableSet<JournalpostId>) {

        hendelseProducer.sendTilKafkaTopic(PLEIEPENGER_SYKT_BARN_TOPIC, søknad, søknad.søknadId.id, journalpostIder)
    }


}
