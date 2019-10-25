package no.nav.k9.pleiepengersyktbarn.soknad

import no.nav.k9.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
internal class PleiepengerSyktBarnSoknadService {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(PleiepengerSyktBarnSoknadService::class.java)
    }

    internal suspend fun komplettSøknadMedEnInnsending(
            innsending: Innsending
    ) {
        logger.info("komplettSøknadMedEnInnsending")
        logger.info("Innsending=$innsending")
    }

    internal suspend fun komplettSøknadMedMappe(
            mappe: Mappe
    ) {
        logger.info("komplettSøknadMedMappe")
        logger.info("Mappe=$mappe")
    }
}
