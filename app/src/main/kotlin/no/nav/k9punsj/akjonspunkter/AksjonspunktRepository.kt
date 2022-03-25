package no.nav.k9punsj.akjonspunkter

import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9punsj.domenetjenester.dto.JournalpostId
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.sql.DataSource

@Repository
class AksjonspunktRepository(private val dataSource: DataSource) {

    companion object {
        const val AKSJONSPUNKT_TABLE = "aksjonspunkt"
    }

    suspend fun opprettAksjonspunkt(aksjonspunktEntitet: AksjonspunktEntitet): AksjonspunktEntitet {
        return using(sessionOf(dataSource)) {
            return@using it.transaction { tx ->
                val aksjonspunktId = tx.run(
                    queryOf(
                        """select aksjonspunkt_id from $AKSJONSPUNKT_TABLE where id_journalpost = ? and aksjonspunkt_status = ? and aksjonspunkt_kode = ?""",
                        aksjonspunktEntitet.journalpostId,
                        aksjonspunktEntitet.aksjonspunktStatus.kode,
                        aksjonspunktEntitet.aksjonspunktKode.kode
                    )
                        .map { row ->
                            row.string("aksjonspunkt_id")
                        }.asSingle
                )
                if (aksjonspunktId == null) {
                    tx.run(
                        queryOf(
                            """
                    insert into $AKSJONSPUNKT_TABLE as a (aksjonspunkt_id, aksjonspunkt_kode, id_journalpost, aksjonspunkt_status, frist_tid, vent_aarsak)
                    values (:aksjonspunkt_id, :aksjonspunkt_kode, :id_journalpost, :aksjonspunkt_status, :frist_tid, :vent_aarsak)
                    """, mapOf(
                                "aksjonspunkt_id" to UUID.fromString(aksjonspunktEntitet.aksjonspunktId),
                                "aksjonspunkt_kode" to aksjonspunktEntitet.aksjonspunktKode.kode,
                                "id_journalpost" to aksjonspunktEntitet.journalpostId,
                                "aksjonspunkt_status" to aksjonspunktEntitet.aksjonspunktStatus.kode,
                                "frist_tid" to aksjonspunktEntitet.frist_tid,
                                "vent_aarsak" to if (aksjonspunktEntitet.vent_årsak != null) aksjonspunktEntitet.vent_årsak.kode else null)
                        ).asUpdate
                    )
                } else {
                    tx.run(queryOf("UPDATE $AKSJONSPUNKT_TABLE SET FRIST_TID = ? WHERE AKSJONSPUNKT_ID = ?",
                        aksjonspunktEntitet.frist_tid,
                        UUID.fromString(aksjonspunktId)).asUpdate)
                }
                aksjonspunktEntitet
            }
        }
    }

    suspend fun settStatus(aksjonspunktId: AksjonspunktId, aksjonspunktStatus: AksjonspunktStatus) {
        return using(sessionOf(dataSource)) {
            it.run(queryOf("UPDATE $AKSJONSPUNKT_TABLE SET AKSJONSPUNKT_STATUS = ? WHERE AKSJONSPUNKT_ID = ?",
                aksjonspunktStatus.kode,
                UUID.fromString(aksjonspunktId)).asUpdate)
        }
    }

    suspend fun hentAksjonspunkterDerFristenHarLøptUt(): List<AksjonspunktEntitet> {
        return using(sessionOf(dataSource)) {
            it.run(
                queryOf("""
                        select aksjonspunkt_id, aksjonspunkt_kode, id_journalpost, aksjonspunkt_status, frist_tid, vent_aarsak
                        from $AKSJONSPUNKT_TABLE
                        where frist_tid IS NOT NULL and frist_tid < now() and aksjonspunkt_status = 'OPPR'
                    """)
                    .map { row ->
                        aksjonspunktEntitet(row)
                    }.asList
            )
        }
    }

    suspend fun hentAlleAksjonspunkter(journalpostId: JournalpostId): List<AksjonspunktEntitet> {
        return using(sessionOf(dataSource)) {
            it.run(
                queryOf("""
                        select aksjonspunkt_id, aksjonspunkt_kode, id_journalpost, aksjonspunkt_status, frist_tid, vent_aarsak
                        from $AKSJONSPUNKT_TABLE
                        where id_journalpost = :journalpostId
                    """, mapOf("journalpostId" to journalpostId))
                    .map { row ->
                        aksjonspunktEntitet(row)
                    }.asList
            )
        }
    }

    fun hentAksjonspunkt(journalpostId: String, kode: String): AksjonspunktEntitet? {
        return using(sessionOf(dataSource)) {
            it.run(
                queryOf("""select aksjonspunkt_id, aksjonspunkt_kode, id_journalpost, aksjonspunkt_status, frist_tid, vent_aarsak
                        from $AKSJONSPUNKT_TABLE
                        where id_journalpost = ? and aksjonspunkt_kode = ?""", journalpostId, kode
                )
                    .map { row ->
                        aksjonspunktEntitet(row)
                    }.asSingle
            )
        }
    }

    private fun aksjonspunktEntitet(row: Row) = AksjonspunktEntitet(
        aksjonspunktId = row.string("aksjonspunkt_id"),
        aksjonspunktKode = AksjonspunktKode.fraKode(row.string("aksjonspunkt_kode")),
        journalpostId = row.string("id_journalpost"),
        aksjonspunktStatus = AksjonspunktStatus.fraKode(row.string("aksjonspunkt_status")),
        frist_tid = row.localDateTimeOrNull("frist_tid"),
        vent_årsak = if (row.stringOrNull("vent_aarsak") != null) VentÅrsakType.fraKode(row.string("vent_aarsak")) else null
    )
}
