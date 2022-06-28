package no.nav.k9punsj.sak

import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.integrasjoner.k9sak.Fagsak
import no.nav.k9punsj.integrasjoner.k9sak.K9SakService
import no.nav.k9punsj.sak.dto.SakInfoDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
internal class SakService(
    private val k9SakService: K9SakService,
    private val personService: PersonService
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(SakService::class.java)
    }

    suspend fun hentSaker(søkerIdent: String): List<SakInfoDto> {
        logger.info("Henter fagsaker fra k9...")
        val (fagsaker: Set<Fagsak>?, feil: String?) = k9SakService.hentFagsaker(søkerIdent)

        if (!feil.isNullOrBlank()) {
            throw IllegalStateException(feil)
        } else {
            return fagsaker!!.map {
                val personIdent = it.pleietrengendeAktorId?.let { aktørId ->
                    personService.finnEllerOpprettPersonVedAktørId(aktørId).norskIdent
                }
                SakInfoDto(
                    fagsakId = it.saksnummer,
                    sakstype = it.sakstype.kode,
                    pleietrengendeIdent = personIdent
                )
            }
        }
    }
}
