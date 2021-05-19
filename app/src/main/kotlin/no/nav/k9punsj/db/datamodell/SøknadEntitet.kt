package no.nav.k9punsj.db.datamodell

import java.time.LocalDate
import java.time.LocalDateTime

typealias SøknadId = String
typealias JsonB = MutableMap<String, Any?>

data class SøknadEntitet(
    val søknadId: SøknadId,
    val bunkeId: BunkeId,
    val søkerId: PersonId,
    val barnId: PersonId? = null,
    val saksnummerK9Sak : String? = null,
    val barnFødselsdato: LocalDate? = null,
    val søknad : JsonB? = null,
    val journalposter: JsonB? = null,
    val sendtInn: Boolean = false,
    val opprettet_av: String? = null,
    val opprettet_tid: LocalDateTime? = null,
    val endret_av: String? = null,
    val endret_tid : LocalDateTime? = null
)
