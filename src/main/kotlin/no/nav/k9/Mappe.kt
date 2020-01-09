package no.nav.k9

import org.springframework.stereotype.Service
import java.util.*

typealias MappeId = String

data class Mappe(
        val mappeId: MappeId,
        val søknadType: SøknadType,
        val innsending: MutableMap<NorskIdent, Person>
)

data class MapperDTO(
        val mapper : List<MappeDTO>
)

data class MappeDTO(
        val mappeId: MappeId,
        val personer: MutableMap<NorskIdent, PersonDTO>
) {
    internal fun erKomplett() = personer.all { it.value.mangler.isEmpty() }
}

data class Person(
        val innsendinger: MutableSet<JournalpostId>,
        val soeknad: Søknad
)

data class PersonDTO(
        val innsendinger: MutableSet<JournalpostId>,
        val soeknad: Søknad,
        val mangler: Set<Mangel>
)

internal fun Mappe.dto(personMangler: Map<NorskIdent, Set<Mangel>>) : MappeDTO {
    val personer = mutableMapOf<NorskIdent, PersonDTO>()
    personMangler.forEach { (norskIdent, mangler) ->
        personer[norskIdent] = PersonDTO(
                innsendinger = innsending[norskIdent]!!.innsendinger,
                soeknad = innsending[norskIdent]!!.soeknad,
                mangler = mangler
        )
    }

    return MappeDTO (
            mappeId = mappeId,
            personer = personer
        )
}


private fun JournalpostInnhold.leggIUndermappe(
        person: Person?
) : Person {
    return Person(
            innsendinger = person?.innsendinger?.leggTil(journalpostId) ?: mutableSetOf(journalpostId),
            soeknad = person?.soeknad?.merge(soeknad) ?: soeknad
    )
}

internal fun Innsending.leggIMappe(
        mappe: Mappe?,
        søknadType: SøknadType? = null
) : Mappe {
    val personligInnholdUndermapper = mappe?.innsending?: mutableMapOf()
    personer?.forEach { (norskIdent, journalpostInnhold) ->
        personligInnholdUndermapper[norskIdent] = journalpostInnhold.leggIUndermappe(person = mappe?.innsending?.get(norskIdent))
    }

    return Mappe(
            mappeId = mappe?.mappeId ?: UUID.randomUUID().toString(),
            søknadType = mappe?.søknadType ?: søknadType!!,
            innsending = personligInnholdUndermapper
    )
}

private fun <E> MutableSet<E>.leggTil(item: E): MutableSet<E> {
    add(item)
    return this
}

@Service
internal class MappeService {
    private val map = mutableMapOf<MappeId, Mappe>()

    internal suspend fun hent(
            norskeIdenter: Set<NorskIdent>,
            søknadType: SøknadType
    ) = map.filterValues { it.innsending.containsKeys(norskeIdenter) }.map { (_, mappe) ->
        mappe
    }.toSet()

    internal suspend fun førsteInnsending(
            søknadType: SøknadType,
            innsending: Innsending
    ) : Mappe {
        val opprettetMappe = innsending.leggIMappe(mappe = null, søknadType = søknadType);

        map[opprettetMappe.mappeId] = opprettetMappe

        return opprettetMappe
    }

    internal suspend fun utfyllendeInnsending(
            mappeId: MappeId,
            søknadType: SøknadType,
            innsending: Innsending
    ) : Mappe? {
        val eksisterendeMappe = map[mappeId]?: return null
        val oppdatertMappe = innsending.leggIMappe(mappe = eksisterendeMappe)

        map[mappeId] = oppdatertMappe

        return oppdatertMappe
    }

    internal suspend fun hent(
            mappeId: MappeId
    ) = map[mappeId]


    internal suspend fun fjern(
            mappeId: MappeId,
            norskIdent: NorskIdent
    ) {
        val mappe = hent(mappeId)?:return
        if (mappe.innsending.containsKey(norskIdent)) {
            if (mappe.innsending.size == 1) {
                map.remove(mappeId)
            } else {
                mappe.innsending.remove(norskIdent)
                map[mappeId] = mappe
            }
        }
    }
}

private fun <K, V> Map<K, V>.containsKeys(keys: Set<K>): Boolean {
    keys.forEach { key ->
        if (!containsKey(key)) {
            return false
        }
    }
    return true
}
