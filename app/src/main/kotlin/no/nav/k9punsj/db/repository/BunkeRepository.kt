package no.nav.k9punsj.db.repository

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9punsj.db.datamodell.BunkeId
import no.nav.k9punsj.db.datamodell.FagsakYtelseType
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.sql.DataSource

@Repository
class BunkeRepository(private val dataSource: DataSource) {


    suspend fun opprettBunke(mappeId: String, type: FagsakYtelseType): BunkeId {
        return using(sessionOf(dataSource)) {
            return@using it.transaction { tx ->
                val bunkeId = UUID.randomUUID()
                tx.run(
                    queryOf(
                        """
                    insert into bunke as k (bunke_id, id_mappe, ytelse_type)
                    values (:bunke_id, :id_mappe, :ytelse_type)
                 """, mapOf("bunke_id" to bunkeId,
                            "id_mappe" to UUID.fromString(mappeId),
                            "ytelse_type" to type.kode)
                    ).asUpdate
                )
                return@transaction bunkeId.toString()
            }
        }
    }
}
