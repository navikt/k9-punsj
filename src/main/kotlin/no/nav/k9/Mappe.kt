package no.nav.k9

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.stereotype.Service
import java.util.*

typealias MappeId = String
//typealias SakId = String

data class Mappe(
        val mappeId: MappeId,
        //val sistEndret: ZonedDateTime,
        //val sakId: SakId?,
        val norskIdent: NorskIdent,
        val innholdType: InnholdType,
        val innhold: Innhold,
        val innsendinger: MutableSet<JournalpostId>
        //val utsendinger: MutableSet<JournalpostId>
)

internal fun Mappe.dto(mangler: Set<Mangel>) = MappeDTO (
        mappeId = mappeId,
        innhold = innhold,
        innsendinger = innsendinger,
        mangler = mangler
)

data class MappeDTO(
        @JsonProperty("mappe_id")
        val mappeId: MappeId,
        val innhold: Innhold,
        val innsendinger: MutableSet<JournalpostId>,
        val mangler: Set<Mangel>
)

@Service
internal class MappeService {

    private val map = mutableMapOf<Pair<NorskIdent, InnholdType>, MutableSet<Mappe>>()

    internal suspend fun hent(
            norskIdent: NorskIdent,
            innholdType: InnholdType
    ) : Set<Mappe> {
        return map[Pair(norskIdent, innholdType)]?.toSet() ?: emptySet()
    }

    internal suspend fun førsteInnsending(
            innholdType: InnholdType,
            innsending: Innsending
    ) : Mappe {
        val key = Pair(innsending.norskIdent, innholdType)
        val mappeId : MappeId = UUID.randomUUID().toString()
        val mappe = Mappe(
                mappeId = mappeId,
                norskIdent = innsending.norskIdent,
                innholdType = innholdType,
                innhold = innsending.innhold,
                innsendinger = mutableSetOf(innsending.journalpostId)
        )

        val mapper = map.getOrDefault(key, mutableSetOf())
        mapper.add(mappe)
        map[key] = mapper

        return mappe
    }

    internal suspend fun utfyllendeInnsending(
            mappeId: MappeId,
            innholdType: InnholdType,
            innsending: Innsending
    ) : Mappe? {
        val key = Pair(innsending.norskIdent, innholdType)
        val mappe = map[key]?.first { it.mappeId == mappeId } ?: return null

        mappe.innsendinger.add(innsending.journalpostId)
        mappe.innhold.merge(innsending.innhold)

        return mappe
    }

    internal suspend fun fjern(
            mappeId: MappeId,
            norskIdent: NorskIdent,
            innholdType: InnholdType
    ) {
        val key = Pair(norskIdent, innholdType)
        map[key]?.apply {
            this.removeIf{ it.mappeId == mappeId }
        }
    }
}