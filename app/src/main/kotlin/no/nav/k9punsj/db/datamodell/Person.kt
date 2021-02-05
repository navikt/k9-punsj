package no.nav.k9punsj.db.datamodell

typealias PersonId = String
typealias NorskIdent = String
typealias AktørId = String

data class Person(
    val personId: PersonId,
    val norskIdent: NorskIdent,
    val aktørId: AktørId
) {
    init {
        no.nav.k9.sak.typer.PersonIdent(norskIdent)
        no.nav.k9.sak.typer.AktørId(aktørId)
    }
}

internal fun Person.getNorskIdent(): NorskIdent {
    return norskIdent
}

internal fun Person.getAktørId(): AktørId {
    return aktørId
}

internal fun Person.getPersonId(): PersonId {
    return personId;
}



