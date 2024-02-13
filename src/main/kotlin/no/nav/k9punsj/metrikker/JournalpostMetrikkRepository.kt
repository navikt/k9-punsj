package no.nav.k9punsj.metrikker

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9punsj.fordel.K9FordelType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import javax.sql.DataSource

@Repository
class JournalpostMetrikkRepository(private val dataSource: DataSource) {

    companion object {
        const val JOURNALPOST_TABLE = "journalpost"
        private val logger = LoggerFactory.getLogger(JournalpostMetrikkRepository::class.java)
    }

    suspend fun hentAntallFerdigBehandledeJournalposter(ferdigBehandlet: Boolean): Int {
        return using(sessionOf(dataSource)) {
            it.transaction { tx ->
                //language=PostgreSQL
                val antall = tx.run(
                    queryOf(
                        """select count(*) as antall from $JOURNALPOST_TABLE where ferdig_behandlet = :ferdig_behandlet""",
                        mapOf("ferdig_behandlet" to ferdigBehandlet)
                    ).map { row -> row.int("antall") }.asSingle
                )
                return@transaction antall ?: 0
            }
        }
    }

    suspend fun hentAntallJournalposttyper(): List<Pair<Int, K9FordelType>> {
        return using(sessionOf(dataSource)) { session ->
            session.transaction { tx ->
                //language=PostgreSQL
                val antallTyper = tx.run(
                    queryOf(
                        """SELECT count(data -> 'type') as antall, data -> 'type' as type FROM journalpost group by data -> 'type'"""
                    ).map { row ->
                        val antall = row.int("antall")
                        val type = row.stringOrNull("type")?.replace("\"", "") // fjerner ekstra fnutter ""
                        val k9FordelType = type?.let {
                            if ("null" == it) K9FordelType.UKJENT
                            else K9FordelType.fraKode(it)
                        } ?: K9FordelType.UKJENT
                        Pair(antall, k9FordelType)
                    }.asList
                )
                return@transaction antallTyper
            }
        }
    }
}
