package no.nav.k9punsj.db.datamodell

import java.time.LocalDate
import java.time.LocalDateTime

typealias SøknadId = String
typealias JsonB = MutableMap<String, Any?>

data class SøknadEntitet(
    val søknadId: SøknadId,
    val bunkeId: BunkeId,
    val person: PersonId,
    val barn: PersonId? = null,
    val barnFødselsdato: LocalDate? = null,
    val søknad : JsonB,
    val journalposter: JsonB,
    val sendt_inn: Boolean? = null,
    val opprettet_av: String? = null,
    val opprettet_tid: LocalDateTime? = null,
    val endret_av: String? = null,
    val endret_tid : LocalDateTime? = null
)
