package no.nav.k9punsj.sak

import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.integrasjoner.k9sak.dto.Fagsak
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
        val søkerAktørId = personService.finnAktørId(søkerIdent)
        val (fagsaker: Set<Fagsak>?, feil: String?) = k9SakService.hentFagsaker(søkerIdent)

        if (!feil.isNullOrBlank()) {
            throw IllegalStateException(feil)
        } else {
            val fagsaker = fagsaker!!.map {
                val personIdent = it.pleietrengendeAktorId?.let { aktørId ->
                    personService.finnEllerOpprettPersonVedAktørId(aktørId).norskIdent
                }
                SakInfoDto(
                    fagsakId = it.saksnummer,
                    reservert = false,
                    sakstype = it.sakstype.kode,
                    pleietrengendeIdent = personIdent,
                    gyldigPeriode = it.gyldigPeriode
                )
            }
            logger.info("Henter reserverte saksnummere fra k9...")
            val reserverteSaksnummere = k9SakService.hentReserverteSaksnummere(søkerAktørId).map {
                SakInfoDto(
                    fagsakId = it.saksnummer,
                    reservert = true,
                    sakstype = it.ytelseType.kode,
                    pleietrengendeIdent = it.pleietrengendeAktørId?.let { aktørId ->
                        personService.finnEllerOpprettPersonVedAktørId(aktørId).norskIdent
                    },
                    gyldigPeriode = null
                )
            }
            // Returnerer fagsaker og reserverte saksnummere
            return (fagsaker + reserverteSaksnummere).distinctBy { it.fagsakId }
        }
    }
}
