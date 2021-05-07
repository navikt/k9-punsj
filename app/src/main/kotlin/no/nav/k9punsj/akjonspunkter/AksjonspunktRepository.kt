package no.nav.k9punsj.akjonspunkter

import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9punsj.rest.web.JournalpostId
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.sql.DataSource

@Repository
class AksjonspunktRepository(private val dataSource: DataSource) {

    suspend fun opprettAksjonspunkt(aksjonspunktEntitet: AksjonspunktEntitet): AksjonspunktEntitet {
        return using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    """
                    insert into aksjonspunkt as a (aksjonspunkt_id, aksjonspunkt_kode, id_journalpost, aksjonspunkt_status, frist_tid, vent_aarsak)
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
            aksjonspunktEntitet
        }
    }

    suspend fun settStatus(aksjonspunktId: AksjonspunktId, aksjonspunktStatus: AksjonspunktStatus) {
        return using(sessionOf(dataSource)) {
            it.run(queryOf("UPDATE AKSJONSPUNKT SET AKSJONSPUNKT_STATUS = ? WHERE AKSJONSPUNKT_ID = ?",
                aksjonspunktStatus.kode,
                UUID.fromString(aksjonspunktId)).asUpdate)
        }
    }

    suspend fun hentAksjonspunkterDerFristenHarLøptUt(): List<AksjonspunktEntitet> {
        return using(sessionOf(dataSource)) {
            it.run(
                queryOf("""
                        select aksjonspunkt_id, aksjonspunkt_kode, id_journalpost, aksjonspunkt_status, frist_tid, vent_aarsak
                        from aksjonspunkt
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
                        from aksjonspunkt
                        where id_journalpost = :journalpostId
                    """, mapOf("journalpostId" to journalpostId))
                    .map { row ->
                        aksjonspunktEntitet(row)
                    }.asList
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

    fun hentAksjonspunkt(journalpostId: String, kode: String): AksjonspunktEntitet? {
        return using(sessionOf(dataSource)) {
            it.run(
                queryOf("""
                        select aksjonspunkt_id, aksjonspunkt_kode, id_journalpost, aksjonspunkt_status, frist_tid, vent_aarsak
                        from aksjonspunkt
                        where id_journalpost = :journalpostId and aksjonspunkt_kode = :kode
                    """, mapOf("journalpostId" to journalpostId),
                    "kode" to kode
                )
                    .map { row ->
                        aksjonspunktEntitet(row)
                    }.asSingle
            )
        }
    }
}
