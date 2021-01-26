package no.nav.k9.person

import no.nav.k9.sak.typer.AktørId
import no.nav.k9.sak.typer.PersonIdent

typealias PersonId = String

data class Person(
        val personId: PersonId,
        val personIdent: PersonIdent,
        val aktørId: AktørId
)


internal fun Person.getPersonIdent() : PersonIdent {
        return personIdent
}

internal fun Person.getAktørId() : AktørId {
        return aktørId
}

internal fun Person.getPersonId() : PersonId {
        return personId;
}



