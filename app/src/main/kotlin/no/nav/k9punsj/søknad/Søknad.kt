package no.nav.k9punsj.søknad

import no.nav.k9punsj.person.Person
import java.util.*


data class Søknad(

        val søknadId: UUID?,
        val søker: Person,
        val ytelse: Ytelse,



        )
