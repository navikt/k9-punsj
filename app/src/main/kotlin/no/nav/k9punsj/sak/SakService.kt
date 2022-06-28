package no.nav.k9punsj.sak

import no.nav.k9punsj.integrasjoner.k9sak.Fagsak
import no.nav.k9punsj.integrasjoner.k9sak.K9SakService
import no.nav.k9punsj.openapi.OasFeil
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.json

@Service
internal class SakService(
    private val k9SakService: K9SakService
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(SakService::class.java)
    }

    suspend fun hentSaker(søkerIdent: String): ServerResponse {
        return kotlin.runCatching {
            logger.info("Henter fagsaker fra k9...")
            val (fagsaker: Set<Fagsak>?, feil: String?) = k9SakService.hentFagsaker(søkerIdent)
            if (!feil.isNullOrBlank()) throw IllegalStateException(feil)
            else fagsaker!!.map {
                SakInfoDto(
                    fagsakId = it.saksnummer,
                    sakstype = it.sakstype.kode,
                    pleietrengendeAktorid = it.pleietrengendeAktorId
                )
            }
        }.fold(
            onSuccess = {
                logger.info("Saker hentet")
                ServerResponse
                    .status(HttpStatus.OK)
                    .json()
                    .bodyValueAndAwait(it)
            },
            onFailure = {
                logger.error("Feilet med å hente saker.", it)
                ServerResponse
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json()
                    .bodyValueAndAwait(OasFeil(it.message))
            }
        )
    }

    data class SakInfoDto(
        val fagsakId: String,
        val sakstype: String,
        val pleietrengendeAktorid: String?
    )
}
