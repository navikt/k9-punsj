package no.nav.k9punsj.journalpost

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9punsj.db.datamodell.AktørId
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.web.JournalpostId
import no.nav.k9punsj.rest.web.dto.JournalpostIdDto
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.sql.DataSource


@Repository
class JournalpostRepository(private val dataSource: DataSource) {


    private val objectMapper = objectMapper();

    suspend fun lagre(journalpostId: Journalpost, kilde: KildeType = KildeType.FORDEL, function: (Journalpost?) -> Journalpost): Journalpost {
        return using(sessionOf(dataSource)) {
            return@using it.transaction { tx ->
                val json = tx.run(
                    queryOf(
                        "select data from journalpost where JOURNALPOST_ID = :id for update",
                        mapOf("id" to journalpostId.journalpostId)
                    )
                        .map { row ->
                            row.string("data")
                        }.asSingle
                )

                val journalpost = if (!json.isNullOrEmpty()) {
                    function(objectMapper.readValue(json, Journalpost::class.java))
                } else {
                    function(null)
                }
                //language=PostgreSQL
                tx.run(
                    queryOf(
                        """
                    insert into journalpost as k (journalpost_id, data, kilde)
                    values (:id, :data :: jsonb, :kilde)
                    on conflict (JOURNALPOST_ID) do update
                    set data = :data :: jsonb
                 """, mapOf("id" to journalpostId.journalpostId, "data" to objectMapper.writeValueAsString(journalpost), "kilde" to kilde.kode)
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
                        "select data from journalpost where journalpost_id = :journalpostId",
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

    suspend fun finnJournalposterPåPerson(aktørId: AktørId): List<JournalpostId> {
        return using(sessionOf(dataSource)) {
            val statement = queryOf(
                "select journalpost_id from journalpost where data ->> 'aktørId' = '$aktørId'"
            )
            val resultat = it.run(
                statement
                    .map { row ->
                        row.string("journalpost_id")
                    }.asList
            )
            resultat
        }
    }

    suspend fun fantIkke(journalpostId: String): Boolean {
        return using(sessionOf(dataSource)) {
            val run = it.run(
                queryOf(
                    "select journalpost_id from journalpost where journalpost_id = :journalpostId",
                    mapOf("journalpostId" to journalpostId)
                ).map { row -> row.string("journalpost_Id") }.asSingle
            )
            run.isNullOrEmpty()
        }
    }

    suspend fun opprettJournalpost(jp: Journalpost): Journalpost {
        return lagre(jp) {
            jp
        }
    }

    suspend fun settBehandletFerdig(journalpostIder: MutableSet<JournalpostId>) {
        journalpostIder.forEach { ferdig(it) }
    }

    suspend fun ferdig(journalpostId: String) {
        return using(sessionOf(dataSource)) {
            it.transaction { tx ->
                //language=PostgreSQL
                val json = tx.run(
                    queryOf(
                        """
                    insert into journalpost as k (journalpost_id, data, ferdig_behandlet)
                    values (:id, :data :: jsonb, :ferdig)
                    on conflict (JOURNALPOST_ID) do update
                    set ferdig_behandlet = :ferdig
                    
                 """, mapOf("id" to journalpostId, "ferdig" to true)
                    ).asUpdate
                )
            }
        }
    }

    suspend fun settKildeHvisIkkeFinnesFraFør(journalposter: List<JournalpostIdDto>?, aktørId: AktørId) {
        journalposter?.forEach {
            if (fantIkke(it)) {
                val journalpost = Journalpost(UUID.randomUUID(), it, aktørId)
                lagre(journalpost, KildeType.SAKSBEHANDLER) {
                    journalpost
                }
            }
        }
    }
}
