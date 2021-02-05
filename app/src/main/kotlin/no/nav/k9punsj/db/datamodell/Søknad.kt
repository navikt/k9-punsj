package no.nav.k9punsj.db.datamodell

import java.util.UUID


data class Søknad (
    val søknadId: UUID?,
    val søker: Person?,
    val ytelse: Ytelse?
)
