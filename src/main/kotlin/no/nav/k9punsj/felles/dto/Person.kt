package no.nav.k9punsj.felles.dto

import java.time.LocalDate

data class Person(
    val personId: String,
    val norskIdent: String,
    val aktørId: String
)

data class PdlPerson(
    internal val identitetsnummer: String,
    internal val fødselsdato: LocalDate,
    internal val navn: String
)
