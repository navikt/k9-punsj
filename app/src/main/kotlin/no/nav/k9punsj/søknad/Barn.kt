package no.nav.k9punsj.søknad

import no.nav.k9punsj.person.Person
import java.time.LocalDate


data class Barn(
        val person: Person,
        val fødlsesdato: LocalDate

)
