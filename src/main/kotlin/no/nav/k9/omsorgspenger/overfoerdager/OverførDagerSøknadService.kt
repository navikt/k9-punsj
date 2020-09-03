package no.nav.k9.omsorgspenger.overfoerdager

import no.nav.k9.JournalpostId
import no.nav.k9.kafka.HendelseProducer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

@Service
class OverførDagerSøknadService @Autowired constructor(
        val hendelseProducer: HendelseProducer
){
    private companion object {
        const val OMSORGSPENGER_OVERFØR_DAGER_SAØKNAD = "privat-punsjet-omsorgspenger-overførdagersøknad"
    }

    internal suspend fun sendSøknad(søknad: OverførDagerSøknad, journalpostIder: MutableSet<JournalpostId>) {
        hendelseProducer.sendTilKafkaTopic(OMSORGSPENGER_OVERFØR_DAGER_SAØKNAD, søknad, UUID.randomUUID().toString(), journalpostIder)
    }
}
