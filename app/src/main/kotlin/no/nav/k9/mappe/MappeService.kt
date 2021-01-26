package no.nav.k9.mappe

import no.nav.k9.Innsending
import no.nav.k9.NorskIdent
import no.nav.k9.SøknadType

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class MappeService @Autowired constructor(val mappeRepository: MappeRepository) {

    suspend fun hent(mappeId: MappeId): Mappe? {
        return mappeRepository.finneMappe(mappeId)
    }

    suspend fun hentMapper(norskeIdenter: Set<NorskIdent>, søknadType: SøknadType): List<Mappe> {
        return mappeRepository.hent(norskeIdenter)
    }

    suspend fun førsteInnsending(
        søknadType: SøknadType,
        innsending: Innsending
    ): Mappe {
        val opprettetMappe = innsending.leggIMappe(mappe = null, søknadType = søknadType)
        return mappeRepository.oppretteMappe(opprettetMappe)
    }

    suspend fun utfyllendeInnsending(mappeId: MappeId, søknadType: SøknadType, innsending: Innsending ): Mappe? {
        return mappeRepository.lagre(mappeId) {
            val oppdatertMappe = innsending.leggIMappe(mappe = it!!)
            oppdatertMappe
        }
    }

    suspend fun fjern(
        mappeId: MappeId,
        norskIdent: NorskIdent
    ) {
        val mappe = hent(mappeId) ?: return
        if (mappe.person.containsKey(norskIdent)) {
            if (mappe.person.size == 1) {
                mappeRepository.sletteMappe(mappeId)
            } else {
                mappe.person.remove(norskIdent)
                mappeRepository.lagre(mappeId) {
                    mappe
                }
            }
        }
    }

    suspend fun slett(mappeid: MappeId) {
        mappeRepository.sletteMappe(mappeid)
    }
}
