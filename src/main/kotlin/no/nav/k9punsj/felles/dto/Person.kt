package no.nav.k9punsj.felles.dto

import java.time.LocalDate

data class Person(
    val personId: String,
    val norskIdent: String,
    val aktørId: String
)

data class PdlPerson(
    val identitetsnummer: String,
    val fødselsdato: LocalDate,
    val navn: String
)
