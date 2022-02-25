package no.nav.k9punsj.journalpost

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9punsj.fordel.PunsjInnsendingType
import org.springframework.stereotype.Repository
import javax.sql.DataSource

@Repository
class JournalpostMetrikkRepository(private val dataSource: DataSource) {

    companion object {
        const val JOURNALPOST_TABLE = "journalpost"
    }

    suspend fun hentAntallFerdigBehandledeJournalposter(ferdigBehandlet: Boolean): Int {
        return using(sessionOf(dataSource)) {
            it.transaction { tx ->
                //language=PostgreSQL
                val antall = tx.run(
                    queryOf(
                        """select count(*) as antall from ${JOURNALPOST_TABLE} where ferdig_behandlet = :ferdig_behandlet""",
                        mapOf("ferdig_behandlet" to ferdigBehandlet)
                    ).map { row -> row.int("antall") }.asSingle
                )
                return@transaction antall ?: 0
            }
        }
    }

    suspend fun hentAntallJournalposttyper(): List<Pair<Int, PunsjInnsendingType>> {
        return using(sessionOf(dataSource)) {
            it.transaction { tx ->
                //language=PostgreSQL
                val antallTyper = tx.run(
                    queryOf(
                        """SELECT count(data -> 'type'), data -> 'type' FROM journalpost group by data -> 'type'"""
                    ).map { row ->
                        Pair(
                            row.int(1),
                            PunsjInnsendingType.fraKode(row.string(2).replace("\"", "")) // fjerner ekstra fnutter ""
                        )
                    }.asList
                )
                return@transaction antallTyper
            }
        }
    }
}
