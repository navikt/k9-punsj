package no.nav.k9punsj.db.datamodell

import java.time.LocalDateTime

open class BaseEntitet(
    val opprettet_av: String,
    val opprettet_tid: LocalDateTime,
    val endret_av: String,
    val endret_tid : LocalDateTime
) {
}
