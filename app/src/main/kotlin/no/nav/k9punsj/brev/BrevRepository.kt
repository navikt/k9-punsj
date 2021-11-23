package no.nav.k9punsj.brev

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9punsj.objectMapper
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.sql.DataSource

@Repository
class BrevRepository(private val dataSource: DataSource) {

   suspend fun opprettBrev(brev: BrevEntitet): BrevEntitet {
        return using(sessionOf(dataSource)) {
            return@using it.transaction { tx ->
                //language=PostgreSQL
                tx.run(
                    queryOf(
                        """insert into brev as k (brev_id, id_journalpost, brev_type, data)
                    values (:brev_id, :id_journalpost, :brev_type, :data :: jsonb)""", mapOf(
                            "brev_id" to UUID.fromString(brev.brevId),
                            "id_journalpost" to brev.forJournalpostId,
                            "brev_type" to brev.brevType.kode,
                            "data" to brev.brevData.toJsonB())
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
                        "SELECT brev_id, id_journalpost, brev_type, data FROM brev WHERE id_journalpost = :id_journalpost",
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
        brevType = BrevType.fromKode(row.string("brev_type"))
    )
}
