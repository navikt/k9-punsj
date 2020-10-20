package no.nav.k9.mappe

import no.nav.k9.*
import org.springframework.stereotype.Service
import java.util.*

typealias MappeId = String

data class Mappe(
        val mappeId: MappeId,
        val søknadType: SøknadType,
        val person: MutableMap<NorskIdent, Person>
)

data class Person(
        val innsendinger: MutableSet<JournalpostId>,
        val soeknad: SøknadJson
)

internal fun Mappe.dto(personMangler: Map<NorskIdent, Set<Mangel>>): MappeSvarDTO {
    val personer = mutableMapOf<NorskIdent, PersonDTO<SøknadJson>>()
    personMangler.forEach { (norskIdent, mangler) ->
        personer[norskIdent] = PersonDTO(
                innsendinger = person[norskIdent]!!.innsendinger,
                soeknad = person[norskIdent]!!.soeknad,
                mangler = mangler
        )
    }

    return MappeSvarDTO(
            mappeId = mappeId,
            personer = personer
    )
}

internal fun Mappe.getFirstNorskIdent(): NorskIdent {
    return this.person.keys.first();
}

internal fun Mappe.getFirstPerson(): Person? {
    return this.person[this.getFirstNorskIdent()];
}

private fun JournalpostInnhold<SøknadJson>.leggIUndermappe(
        person: Person?
): Person {
    return Person(
            innsendinger = person?.innsendinger?.leggTil(journalpostId) ?: mutableSetOf(journalpostId),
            soeknad = person?.soeknad?.merge(soeknad) ?: soeknad
    )
}

internal fun Innsending.leggIMappe(
        mappe: Mappe?,
        søknadType: SøknadType? = null
): Mappe {
    val personligInnholdUndermapper = mappe?.person ?: mutableMapOf()
    personer?.forEach { (norskIdent, journalpostInnhold) ->
        personligInnholdUndermapper[norskIdent] = journalpostInnhold.leggIUndermappe(person = mappe?.person?.get(norskIdent))
    }

    return Mappe(
            mappeId = mappe?.mappeId ?: UUID.randomUUID().toString(),
            søknadType = mappe?.søknadType ?: søknadType!!,
            person = personligInnholdUndermapper
    )
}

private fun <E> MutableSet<E>.leggTil(item: E): MutableSet<E> {
    add(item)
    return this
}

private fun <K, V> Map<K, V>.containsKeys(keys: Set<K>): Boolean {
    keys.forEach { key ->
        if (!containsKey(key)) {
            return false
        }
    }
    return true
}
