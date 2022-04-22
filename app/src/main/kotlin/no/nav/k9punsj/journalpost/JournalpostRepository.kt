package no.nav.k9punsj.journalpost

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.felles.PunsjJournalpostKildeType
import no.nav.k9punsj.fordel.PunsjInnsendingType
import no.nav.k9punsj.objectMapper
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.sql.DataSource

@Repository
class JournalpostRepository(private val dataSource: DataSource) {

    companion object {
        const val JOURNALPOST_TABLE = "journalpost"
    }

    private val objectMapper = objectMapper()

    internal suspend fun lagre(
        punsjJournalpostId: PunsjJournalpost,
        kilde: PunsjJournalpostKildeType = PunsjJournalpostKildeType.FORDEL,
        function: (PunsjJournalpost?) -> PunsjJournalpost,
    ): PunsjJournalpost {
        return using(sessionOf(dataSource)) {
            return@using it.transaction { tx ->
                val json = tx.run(
                    queryOf(
                        "select data from $JOURNALPOST_TABLE where JOURNALPOST_ID = :id for update",
                        mapOf("id" to punsjJournalpostId.journalpostId)
                    )
                        .map { row ->
                            row.string("data")
                        }.asSingle
                )

                val punsjJournalpost = if (!json.isNullOrEmpty()) {
                    function(objectMapper.readValue(json, PunsjJournalpost::class.java))
                } else {
                    function(null)
                }
                //language=PostgreSQL
                tx.run(
                    queryOf(
                        """
                    insert into $JOURNALPOST_TABLE as k (journalpost_id, data, kilde)
                    values (:id, :data :: jsonb, :kilde)
                    on conflict (JOURNALPOST_ID) do update
                    set data = :data :: jsonb
                 """,
                        mapOf("id" to punsjJournalpostId.journalpostId,
                            "data" to objectMapper.writeValueAsString(punsjJournalpost),
                            "kilde" to kilde.kode)
                    ).asUpdate
                )
                return@transaction punsjJournalpost
            }
        }
    }

    suspend fun hent(journalpostId: String): PunsjJournalpost {
        return using(sessionOf(dataSource)) {
            it.transaction { tx ->
                //language=PostgreSQL
                val json = tx.run(
                    queryOf(
                        "select data from $JOURNALPOST_TABLE where journalpost_id = :journalpostId",
                        mapOf("journalpostId" to journalpostId)
                    )
                        .map { row ->
                            row.string("data")
                        }.asSingle
                )
                return@transaction objectMapper.readValue(json, PunsjJournalpost::class.java)
            }
        }
    }

    suspend fun hentHvis(journalpostId: String): PunsjJournalpost? {
        return using(sessionOf(dataSource)) {
            it.transaction { tx ->
                //language=PostgreSQL
                val json = tx.run(
                    queryOf(
                        "select data from $JOURNALPOST_TABLE where journalpost_id = :journalpostId",
                        mapOf("journalpostId" to journalpostId)
                    )
                        .map { row ->
                            row.string("data")
                        }.asSingle
                )
                if (json != null) {
                    return@transaction objectMapper.readValue(json, PunsjJournalpost::class.java)

                }
                return@transaction null
            }
        }
    }

    suspend fun finnJournalposterPåPerson(aktørId: String): List<PunsjJournalpost> {
        return using(sessionOf(dataSource)) {
            val statement = queryOf(
                "SELECT DATA FROM $JOURNALPOST_TABLE WHERE data ->> 'aktørId' = '$aktørId' AND FERDIG_BEHANDLET IS FALSE"
            )
            val resultat = it.run(
                statement
                    .map { row ->
                        row.string("data")
                    }.asList
            )
            resultat.map { res ->  objectMapper.readValue(res, PunsjJournalpost::class.java) }
        }
    }

    suspend fun finnJournalposterPåPersonBareFordel(aktørId: String): List<PunsjJournalpost> {
        return using(sessionOf(dataSource)) {
            val statement = queryOf(
                "SELECT DATA FROM $JOURNALPOST_TABLE WHERE data ->> 'aktørId' = '$aktørId' AND FERDIG_BEHANDLET IS FALSE AND KILDE = 'FORDEL'"
            )
            val resultat = it.run(
                statement
                    .map { row ->
                        row.string("data")
                    }.asList
            )
            resultat.map { res ->  objectMapper.readValue(res, PunsjJournalpost::class.java) }
        }
    }

    suspend fun journalpostIkkeEksisterer(journalpostId: String): Boolean {
        return using(sessionOf(dataSource)) {
            val run = it.run(
                queryOf(
                    "select journalpost_id from $JOURNALPOST_TABLE where journalpost_id = :journalpostId",
                    mapOf("journalpostId" to journalpostId)
                ).map { row -> row.string("journalpost_Id") }.asSingle
            )
            run.isNullOrEmpty()
        }
    }

    suspend fun opprettJournalpost(jp: PunsjJournalpost): PunsjJournalpost {
        return lagre(jp) {
            jp
        }
    }

    suspend fun ferdig(journalpostId: String) {
        return using(sessionOf(dataSource)) {
            it.run(queryOf("UPDATE $JOURNALPOST_TABLE SET FERDIG_BEHANDLET = true, endret_tid = now(), endret_av = 'PUNSJ' where JOURNALPOST_ID = ?",
                journalpostId).asUpdate)
        }
    }

    suspend fun settAlleTilFerdigBehandlet(journalpostIder: List<String>) {
        return using(sessionOf(dataSource)) {
            it.transaction { tx ->
                val antallUpdates = using(sessionOf(dataSource)) {
                    tx.run(queryOf("UPDATE $JOURNALPOST_TABLE SET FERDIG_BEHANDLET = true, endret_tid = now(), endret_av = 'PUNSJ' where JOURNALPOST_ID in (${
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

    suspend fun settKildeHvisIkkeFinnesFraFør(journalposter: List<String>?, aktørId: String) {
        journalposter?.forEach {
            if (journalpostIkkeEksisterer(it)) {
                val punsjJournalpost = PunsjJournalpost(UUID.randomUUID(), it, aktørId)
                lagre(punsjJournalpost, PunsjJournalpostKildeType.SAKSBEHANDLER) {
                    punsjJournalpost
                }
            }
        }
    }

    suspend fun settFagsakYtelseType(ytelseType: FagsakYtelseType, journalpostId: String) {
        val journalpost = hentHvis(journalpostId)
        if (journalpost != null) {
            val medType = journalpost.copy(ytelse = ytelseType.kode)
            lagre(medType){
                medType
            }
        }
    }

    suspend fun settInnsendingstype(type: PunsjInnsendingType, journalpostId: String) {
        val journalpost = hentHvis(journalpostId)
        if (journalpost != null) {
            val medType = journalpost.copy(type = type.kode)
            lagre(medType){
                medType
            }
        }
    }

    fun kanSendeInn(journalpostIder: List<String>): Boolean {
        val using = using(sessionOf(dataSource)) {
            it.run(queryOf("select ferdig_behandlet from $JOURNALPOST_TABLE where journalpost_id in (${
                IntRange(0, journalpostIder.size - 1).joinToString { t -> ":p$t" }
            })",
                IntRange(0,
                    journalpostIder.size - 1).associate { t -> "p$t" to journalpostIder[t] as Any }).map { row ->
                row.boolean("ferdig_behandlet")
            }.asList)
        }

        return !using.contains(true)
    }
}
