package no.nav.k9punsj.sak

import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.integrasjoner.k9sak.dto.Fagsak
import no.nav.k9punsj.integrasjoner.k9sak.K9SakService
import no.nav.k9punsj.sak.dto.SakInfoDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SakService(
    private val k9SakService: K9SakService,
    private val personService: PersonService,
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
                val pleietrengende = it.pleietrengendeAktorId?.let { aktørId ->
                    personService.hentPersonopplysninger(aktørId)
                }

                val relatertPersonIdent = it.relatertPersonAktørId?.let { aktørId ->
                    personService.finnEllerOpprettPersonVedAktørId(aktørId).norskIdent
                }

                val relatertPerson = it.relatertPersonAktørId?.let { aktørId ->
                    personService.hentPersonopplysninger(aktørId)
                }

                val gyldigPeriode = it.gyldigPeriode
                SakInfoDto(
                    reservert = false,
                    fagsakId = it.saksnummer,
                    sakstype = it.sakstype.kode,
                    pleietrengendeIdent = pleietrengende?.identitetsnummer,
                    pleietrengende = pleietrengende,
                    relatertPersonIdent = relatertPersonIdent,
                    relatertPerson = relatertPerson,
                    gyldigPeriode = gyldigPeriode,
                    behandlingsår = gyldigPeriode?.fom?.year
                )
            }
            logger.info("Henter reserverte saksnummere fra k9...")
            val reserverteSaksnummere = k9SakService.hentReserverteSaksnummere(søkerAktørId).map {
                val pleietrengende = it.pleietrengendeAktørId?.let { aktørId ->
                    personService.hentPersonopplysninger(aktørId)
                }

                val relatertPerson = it.relatertPersonAktørId?.let { aktørId ->
                    personService.hentPersonopplysninger(aktørId)
                }

                SakInfoDto(
                    reservert = true,
                    fagsakId = it.saksnummer,
                    sakstype = it.ytelseType.kode,
                    pleietrengendeIdent = pleietrengende?.identitetsnummer,
                    pleietrengende = pleietrengende,
                    gyldigPeriode = null,
                    relatertPersonIdent = relatertPerson?.identitetsnummer,
                    relatertPerson = relatertPerson,
                    behandlingsår = it.behandlingsår
                )
            }
            // Returnerer fagsaker og reserverte saksnummere
            return (fagsaker + reserverteSaksnummere).distinctBy { it.fagsakId }
        }
    }
}
