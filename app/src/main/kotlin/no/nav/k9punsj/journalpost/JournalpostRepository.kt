package no.nav.k9punsj.journalpost

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9punsj.db.datamodell.AktørId
import no.nav.k9punsj.db.datamodell.JsonB
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.web.JournalpostId
import no.nav.k9punsj.rest.web.dto.JournalpostIdDto
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource


@Repository
class JournalpostRepository(private val dataSource: DataSource) {


    private val objectMapper = objectMapper();

    suspend fun lagre(
        journalpostId: Journalpost,
        kilde: KildeType = KildeType.FORDEL,
        function: (Journalpost?) -> Journalpost,
    ): Journalpost {
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
                 """,
                        mapOf("id" to journalpostId.journalpostId,
                            "data" to objectMapper.writeValueAsString(journalpost),
                            "kilde" to kilde.kode)
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

    suspend fun hentHvis(journalpostId: String): Journalpost? {
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
                if (json != null) {
                    return@transaction objectMapper.readValue(json, Journalpost::class.java)

                }
                return@transaction null
            }
        }
    }

    suspend fun finnJournalposterPåPerson(aktørId: AktørId): List<Journalpost> {
        return using(sessionOf(dataSource)) {
            val statement = queryOf(
                "SELECT DATA FROM JOURNALPOST WHERE data ->> 'aktørId' = '$aktørId' AND FERDIG_BEHANDLET IS FALSE"
            )
            val resultat = it.run(
                statement
                    .map { row ->
                        row.string("data")
                    }.asList
            )
            resultat.map { res ->  objectMapper.readValue(res, Journalpost::class.java) }
        }
    }

    suspend fun finnJournalposterPåPersonBareFordel(aktørId: AktørId): List<Journalpost> {
        return using(sessionOf(dataSource)) {
            val statement = queryOf(
                "SELECT DATA FROM JOURNALPOST WHERE data ->> 'aktørId' = '$aktørId' AND FERDIG_BEHANDLET IS FALSE AND KILDE = 'FORDEL'"
            )
            val resultat = it.run(
                statement
                    .map { row ->
                        row.string("data")
                    }.asList
            )
            resultat.map { res ->  objectMapper.readValue(res, Journalpost::class.java) }
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

    suspend fun ferdig(journalpostId: String) {
        return using(sessionOf(dataSource)) {
            it.run(queryOf("UPDATE JOURNALPOST SET FERDIG_BEHANDLET = true, endret_tid = now(), endret_av = 'PUNSJ' where JOURNALPOST_ID = ?",
                journalpostId).asUpdate)
        }
    }

    suspend fun settAlleTilFerdigBehandlet(journalpostIder: List<JournalpostId>) {
        return using(sessionOf(dataSource)) {
            it.transaction { tx ->
                val antallUpdates = using(sessionOf(dataSource)) {
                    tx.run(queryOf("UPDATE JOURNALPOST SET FERDIG_BEHANDLET = true, endret_tid = now(), endret_av = 'PUNSJ' where JOURNALPOST_ID in (${
                        IntRange(0, journalpostIder.size - 1).joinToString { t -> ":p$t" }
                    })",
                        IntRange(0,
                            journalpostIder.size - 1).associate { t -> "p$t" to journalpostIder[t] as Any }).asUpdate)
                }
                if (antallUpdates != journalpostIder.size) {
                    throw IllegalStateException("Klarte ikke sette alle til ferdig")
                }
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

    fun kanSendeInn(journalpostIder: List<JournalpostId>): Boolean {
        val using = using(sessionOf(dataSource)) {
            it.run(queryOf("select ferdig_behandlet from journalpost where journalpost_id in (${
                IntRange(0, journalpostIder.size - 1).joinToString { t -> ":p$t" }
            })",
                IntRange(0,
                    journalpostIder.size - 1).associate { t -> "p$t" to journalpostIder[t] as Any }).map { row ->
                row.boolean("ferdig_behandlet")
            }.asList)
        }

        return !using.contains(true)
    }

    data class JournalIdMedDato(
        val journalpostId: JournalpostId,
        val dato: LocalDate,
    )
}
