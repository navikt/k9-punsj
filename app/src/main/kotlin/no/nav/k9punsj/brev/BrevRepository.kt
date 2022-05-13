package no.nav.k9punsj.brev

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9punsj.brev.DokumentbestillingDto.Companion.toJsonB
import no.nav.k9punsj.objectMapper
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.sql.DataSource

@Repository
class BrevRepository(private val dataSource: DataSource) {

    companion object {
        const val BREV_TABLE = "brev"
    }

   suspend fun opprettBrev(brev: BrevEntitet, saksbehandler: String): BrevEntitet {
        return using(sessionOf(dataSource)) {
            return@using it.transaction { tx ->
                //language=PostgreSQL
                tx.run(
                    queryOf(
                        """insert into $BREV_TABLE as k (brev_id, id_journalpost, brev_type, data, opprettet_av)
                    values (:brev_id, :id_journalpost, :brev_type, :data :: jsonb, :opprettet_av)""", mapOf(
                            "brev_id" to UUID.fromString(brev.brevId),
                            "id_journalpost" to brev.forJournalpostId,
                            "brev_type" to brev.brevType.kode,
                            "data" to brev.brevData.toJsonB(),
                            "opprettet_av" to saksbehandler)
                    ).asUpdate
                )
                return@transaction brev
            }
        }
    }

   suspend fun hentAlleBrevPåJournalpost(forJournalpostId: String): List<BrevEntitet> {
        return using(sessionOf(dataSource)) {
            it.transaction { tx ->
                tx.run(
                    queryOf(
                        "SELECT brev_id, id_journalpost, brev_type, data, opprettet_av, opprettet_tid FROM $BREV_TABLE WHERE id_journalpost = :id_journalpost",
                        mapOf("id_journalpost" to forJournalpostId)
                    )
                        .map { row ->
                            brevEntitet(row)
                        }.asList
                )
            }
        }
    }

    private fun brevEntitet(row: Row) = BrevEntitet(
        brevId = row.string("brev_id"),
        forJournalpostId = row.string("id_journalpost"),
        brevData = objectMapper().readValue(row.string("data")),
        brevType = BrevType.fromKode(row.string("brev_type")),
        opprettet_av = row.string("opprettet_av"),
        opprettet_tid = row.localDateTime("opprettet_tid")
    )
}
