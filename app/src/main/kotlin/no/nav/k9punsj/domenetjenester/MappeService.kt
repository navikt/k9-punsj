package no.nav.k9punsj.domenetjenester

import no.nav.k9punsj.db.datamodell.*
import no.nav.k9punsj.db.repository.MappeRepository
import no.nav.k9punsj.rest.web.Innsending
import org.springframework.stereotype.Service

@Service
class MappeService(
    val mappeRepository: MappeRepository,
    val personService: PersonService
) {

    suspend fun hent(mappeId: MappeId): Mappe? {
        return mappeRepository.finneMappe(mappeId)
    }

    suspend fun hentMapper(personIder: Set<PersonId>, søknadType: FagsakYtelseType): List<Mappe> {
        return mappeRepository.hent(personIder)
    }

    suspend fun hentAlleMapper(): List<Mappe> {
        return mappeRepository.hentAlleMapper()
    }

    suspend fun førsteInnsending(
        søknadType: FagsakYtelseType,
        innsending: Innsending
    ): Mappe {
        val personer = innsending.personer.keys.map { p -> personService.finnEllerOpprettPersonVedNorskIdent(p) }
        val opprettetMappe = innsending.leggIMappe(
            mappe = null,
            fagsakYtelseType = søknadType
        ) { norskIdent -> personer.first { p -> p.norskIdent == norskIdent }.personId }
        return mappeRepository.oppretteMappe(opprettetMappe)
    }

    suspend fun utfyllendeInnsending(
        mappeId: MappeId,
        fagsakYtelseType: FagsakYtelseType,
        innsending: Innsending
    ): Mappe? {
        val personer = innsending.personer.keys.map { p -> personService.finnEllerOpprettPersonVedNorskIdent(p) }

        return mappeRepository.lagre(mappeId) { mappe ->
            return@lagre innsending.leggIMappe(mappe = mappe!!) {
                return@leggIMappe personer.first { p -> p.norskIdent == it }.personId
            }
        }
    }

    suspend fun fjern(
        mappeId: MappeId,
        norskIdent: NorskIdent
    ) {
        val mappe = hent(mappeId) ?: return
        if (mappe.personInfo.containsKey(norskIdent)) {
            if (mappe.personInfo.size == 1) {
                mappeRepository.sletteMappe(mappeId)
            } else {
                mappe.personInfo.remove(norskIdent)
                mappeRepository.lagre(mappeId) {
                    return@lagre mappe
                }
            }
        }
    }

    suspend fun slett(mappeid: MappeId) {
        mappeRepository.sletteMappe(mappeid)
    }
}
