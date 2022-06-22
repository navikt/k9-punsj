package no.nav.k9punsj.domenetjenester.repository

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.sql.DataSource

@Repository
class MappeRepository(private val dataSource: DataSource) {
    companion object {
        const val MAPPE_TABLE = "mappe"
    }

    suspend fun opprettEllerHentMappeForPerson(personId: String): String {
        return using(sessionOf(dataSource)) {
            return@using it.transaction { tx ->
                val resultat = tx.run(
                    queryOf(
                        "select id from $MAPPE_TABLE where id_person = :id_person",
                        mapOf("id_person" to UUID.fromString(personId))
                    )
                        .map { row ->
                            row.string("id")
                        }.asSingle
                )
                if (!resultat.isNullOrEmpty()) {
                    return@transaction resultat
                }

                val mappeId = UUID.randomUUID()
                tx.run(
                    queryOf(
                        """
                    insert into $MAPPE_TABLE as k (id, id_person)
                    values (:id, :id_person)
                    
                 """,
                        mapOf("id" to mappeId, "id_person" to UUID.fromString(personId))
                    ).asUpdate
                )
                return@transaction mappeId.toString()
            }
        }
    }
}
