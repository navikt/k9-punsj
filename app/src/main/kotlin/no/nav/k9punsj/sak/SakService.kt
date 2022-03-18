package no.nav.k9punsj.sak

import no.nav.k9punsj.rest.eksternt.k9sak.Fagsak
import no.nav.k9punsj.rest.eksternt.k9sak.K9SakService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SakService(
    private val k9SakService: K9SakService
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(SakService::class.java)
    }

    suspend fun hentSaker(søkerIdent: String): List<SakInfoDto> {
        logger.info("Henter fagsaker fra k9...")
        val (fagsaker: Set<Fagsak>?, feil: String?) = k9SakService.hentFagsaker(søkerIdent)
        if (!feil.isNullOrBlank()) throw IllegalStateException(feil)
        else return fagsaker!!.map {
            SakInfoDto(
                fagsakId = it.saksnummer,
                sakstype = it.sakstype.kode
            )
        }
    }

    data class SakInfoDto(
        val fagsakId: String,
        val sakstype: String
    )
}
