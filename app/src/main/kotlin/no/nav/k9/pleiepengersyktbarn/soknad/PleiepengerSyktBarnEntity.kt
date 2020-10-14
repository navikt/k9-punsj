package no.nav.k9.pleiepengersyktbarn.soknad

import java.sql.Date
import java.sql.Timestamp
import java.util.*

data class PleiepengerSyktBarnEntity(
        val id_soknad: Int?,
        val id_mappe: UUID,
        val norsk_ident: String,
        val sist_endret: Timestamp,
        val barn_norsk_ident: String?,
        val barn_fodselsdato: Date?,
        val soknad: String?
)