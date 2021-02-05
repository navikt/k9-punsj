package no.nav.k9punsj.db.datamodell


import no.nav.k9punsj.rest.web.*
import no.nav.k9punsj.rest.web.dto.MappeSvarDTO
import no.nav.k9punsj.rest.web.dto.PersonDTO
import java.util.UUID

typealias MappeId = String

data class Mappe(
    val mappeId: MappeId,
    val søknadType: FagsakYtelseType,
    val personInfo: MutableMap<PersonId, PersonInfo>
) {
    fun getFørstePerson(): PersonId {
        return personInfo.keys.first()
    }
}

data class PersonInfo(
    val innsendinger: MutableSet<JournalpostId>,
    val soeknad: SøknadJson
)

internal fun Mappe.tilDto(f:(PersonId) -> (NorskIdent)): MappeSvarDTO {
    val map = personInfo.mapKeys { key -> f(key.key) }
        .mapValues { value -> PersonDTO(innsendinger = value.value.innsendinger, soeknad = value.value.soeknad) }
        .toMutableMap()

    return MappeSvarDTO(this.mappeId, map)
}


internal fun Mappe.getFirstPerson(): PersonInfo? {
    return this.personInfo[this.getFørstePerson()];
}

private fun JournalpostInnhold<SøknadJson>.leggIUndermappe(personInfo: PersonInfo?): PersonInfo {
    return PersonInfo(
        innsendinger = personInfo?.innsendinger?.leggTil(journalpostId) ?: mutableSetOf(journalpostId),
        soeknad = personInfo?.soeknad?.mergeNy(soeknad) ?: soeknad
    )
}

internal fun Innsending.leggIMappe(
    mappe: Mappe?,
    fagsakYtelseType: FagsakYtelseType? = null,
    f: (NorskIdent) -> (PersonId)
): Mappe {
    val personligInnholdUndermapper = mappe?.personInfo ?: mutableMapOf()
    this.personer.forEach { (personId, journalpostInnhold) ->
        val f1 = f(personId)
        personligInnholdUndermapper[f1] = journalpostInnhold.leggIUndermappe(personInfo = mappe?.personInfo?.get(f(personId)))
    }
    return Mappe(
        mappeId = mappe?.mappeId ?: UUID.randomUUID().toString(),
        søknadType = mappe?.søknadType ?: fagsakYtelseType!!,
        personInfo = personligInnholdUndermapper
    )
}

private fun <E> MutableSet<E>.leggTil(item: E): MutableSet<E> {
    add(item)
    return this
}
