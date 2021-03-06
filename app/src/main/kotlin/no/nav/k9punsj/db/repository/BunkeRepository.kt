package no.nav.k9punsj.db.repository

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9punsj.db.datamodell.BunkeEntitet
import no.nav.k9punsj.db.datamodell.BunkeId
import no.nav.k9punsj.db.datamodell.FagsakYtelseType
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.sql.DataSource

@Repository
class BunkeRepository(private val dataSource: DataSource) {

    suspend fun opprettEllerHentBunkeForFagsakType(mappeId: String, type: FagsakYtelseType): BunkeId {
        return using(sessionOf(dataSource)) {
            return@using it.transaction { tx ->
                val resultat = tx.run(
                    queryOf(
                        "select bunke_id from bunke where id_mappe = :id_mappe and ytelse_type = :ytelse_type",
                        mapOf("id_mappe" to UUID.fromString(mappeId),
                            "ytelse_type" to type.kode
                        )
                    )
                        .map { row ->
                            row.string("bunke_id")
                        }.asSingle
                )
                if (!resultat.isNullOrEmpty()) {
                    return@transaction resultat
                }

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

    suspend fun hentAlleBunkerForMappe(mappeId: String): List<BunkeEntitet> {
        return using(sessionOf(dataSource)) {
            return@using it.transaction { tx ->
                return@transaction tx.run(
                    queryOf(
                        """
                                select bunke_id, id_mappe, ytelse_type from bunke where id_mappe = :mappeId
                             """, mapOf("mappeId" to UUID.fromString(mappeId)
                        )).map { row ->
                        BunkeEntitet(row.string("bunke_id"), FagsakYtelseType.fromKode(row.string("ytelse_type")))
                    }.asList
                )
            }
        }
    }
}
