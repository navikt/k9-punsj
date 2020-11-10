package no.nav.k9.journalpost

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9.objectMapper
import org.springframework.stereotype.Repository
import javax.sql.DataSource


@Repository
class JournalpostRepository(private val dataSource: DataSource) {


    private val objectMapper = objectMapper();

    suspend fun lagre(journalpostId: Journalpost, f: (Journalpost?) -> Journalpost): Journalpost {
        return using(sessionOf(dataSource)) {
            return@using it.transaction { tx ->
                val json = tx.run(
                        queryOf(
                                "select data from journalpost where JOURNALPOSTID = :id for update",
                                mapOf("id" to journalpostId.uuid)
                        )
                                .map { row ->
                                    row.string("data")
                                }.asSingle
                )

                val journalpost = if (!json.isNullOrEmpty()) {
                    f(objectMapper.readValue(json, Journalpost::class.java))
                } else {
                    f(null)
                }
                //language=PostgreSQL
                tx.run(
                        queryOf(
                                """
                    insert into journalpost as k (JOURNALPOSTID, data)
                    values (:id, :data :: jsonb)
                    on conflict (JOURNALPOSTID) do update
                    set data = :data :: jsonb
                 """, mapOf("id" to journalpostId.uuid, "data" to objectMapper.writeValueAsString(journalpost))
                        ).asUpdate
                )
                return@transaction journalpost
            }
        }
    }

    suspend fun hent(journalpostId: String): Journalpost {
        return using(sessionOf(dataSource)) {
            it.transaction { tx ->
                //language=PostgreSQL
                val json = tx.run(
                        queryOf(
                                "select data from journalpost where journalpostid = :journalpostId",
                                mapOf("journalpostId" to journalpostId)
                        )
                                .map { row ->
                                    row.string("data")
                                }.asSingle
                )
                return@transaction objectMapper.readValue(json, Journalpost::class.java)
            }
        }
    }

    suspend fun opprettJournalpost(jp: Journalpost): Journalpost {
        return lagre(jp) {
            jp
        }
    }
}
