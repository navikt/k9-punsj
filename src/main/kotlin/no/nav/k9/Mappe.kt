package no.nav.k9

import org.springframework.stereotype.Service
import java.util.*

typealias MappeId = String

data class Mappe(
        val mappeId: MappeId,
        val innholdType: InnholdType,
        val personlig: MutableMap<NorskIdent, Undermappe>
)

data class MapperDTO(
        val mapper : List<MappeDTO>
)

data class MappeDTO(
        val mappeId: MappeId,
        val personlig: MutableMap<NorskIdent, UndermappeDTO>
) {
    internal fun erKomplett() = personlig.all { it.value.mangler.isEmpty() }
}

data class Undermappe(
        val innsendinger: MutableSet<JournalpostId>,
        val innhold: Innhold
)

data class UndermappeDTO(
        val innsendinger: MutableSet<JournalpostId>,
        val innhold: Innhold,
        val mangler: Set<Mangel>
)

internal fun Mappe.dto(
        personligMangler: Map<NorskIdent, Set<Mangel>>
) : MappeDTO {
    val personligInnhold = mutableMapOf<NorskIdent, UndermappeDTO>()
    personligMangler.forEach { (norskIdent, mangler) ->
        personligInnhold[norskIdent] = UndermappeDTO(
                innsendinger = personlig[norskIdent]!!.innsendinger,
                innhold = personlig[norskIdent]!!.innhold,
                mangler = mangler
        )
    }

    return MappeDTO (
            mappeId = mappeId,
            personlig = personligInnhold
        )
}


private fun JournalpostInnhold.leggIUndermappe(
        undermappe: Undermappe?
) : Undermappe {
    return Undermappe(
            innsendinger = undermappe?.innsendinger?.leggTil(journalpostId) ?: mutableSetOf(journalpostId),
            innhold = undermappe?.innhold?.merge(innhold) ?: innhold
    )
}

internal fun Innsending.leggIMappe(
        mappe: Mappe?,
        innholdType: InnholdType? = null
) : Mappe {
    val personligInnholdUndermapper = mappe?.personlig?: mutableMapOf()
    personlig?.forEach { (norskIdent, journalpostInnhold) ->
        personligInnholdUndermapper[norskIdent] = journalpostInnhold.leggIUndermappe(undermappe = mappe?.personlig?.get(norskIdent))
    }

    return Mappe(
            mappeId = mappe?.mappeId ?: UUID.randomUUID().toString(),
            innholdType = mappe?.innholdType ?: innholdType!!,
            personlig = personligInnholdUndermapper
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
            innholdType: InnholdType
    ) = map.filterValues { it.personlig.containsKeys(norskeIdenter) }.map { (_, mappe) ->
        mappe
    }.toSet()

    internal suspend fun førsteInnsending(
            innholdType: InnholdType,
            innsending: Innsending
    ) : Mappe {
        val opprettetMappe = innsending.leggIMappe(mappe = null, innholdType = innholdType);

        map[opprettetMappe.mappeId] = opprettetMappe

        return opprettetMappe
    }

    internal suspend fun utfyllendeInnsending(
            mappeId: MappeId,
            innholdType: InnholdType,
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
        if (mappe.personlig.containsKey(norskIdent)) {
            if (mappe.personlig.size == 1) {
                map.remove(mappeId)
            } else {
                mappe.personlig.remove(norskIdent)
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
