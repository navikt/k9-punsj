package no.nav.k9punsj.db.repository

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9punsj.db.datamodell.Mappe
import no.nav.k9punsj.db.datamodell.MappeId
import no.nav.k9punsj.db.datamodell.PersonId
import no.nav.k9punsj.objectMapper
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.sql.DataSource

@Repository
class MappeRepository(private val dataSource: DataSource) {

    suspend fun hent(personIder: Set<PersonId>) : List<Mappe> {
      return using(sessionOf(dataSource)) {
            it.transaction { tx ->
                //language=PostgreSQL
                tx.run(
                        queryOf(
                                "select data from mappe where (data -> 'person') ??| array['${personIder.joinToString("','")}']",
                        )
                                .map { row ->
                                    objectMapper().readValue<Mappe>(row.string("data"))
                                }.asList
                )
            }
        }
    }

    suspend fun oppretteMappe(mappe: Mappe): Mappe {
        return lagre(mappe.mappeId) {
            mappe
        }
    }

    suspend fun finneMappe(mappeId: MappeId): Mappe? {
        return using(sessionOf(dataSource)) {
            it.transaction { tx ->
                tx.run(
                        queryOf(
                                "select data from mappe where id = :id ",
                                mapOf("id" to UUID.fromString(mappeId))
                        )
                                .map { row ->
                                    objectMapper().readValue<Mappe>(row.string("data"))
                                }.asSingle
                )
            }
        }
    }

    suspend fun sletteMappe(mappeId: MappeId) {
        using(sessionOf(dataSource)) {
            it.transaction { tx ->
                tx.run(
                        queryOf(
                                "delete from mappe where id = :id",
                                mapOf("id" to UUID.fromString(mappeId))
                        ).asUpdate
                )
            }
        }
    }

    suspend fun lagre(mappeId: MappeId, f: (Mappe?) -> Mappe): Mappe {
        return using(sessionOf(dataSource)) {
            return@using it.transaction { tx ->
                val json = tx.run(
                        queryOf(
                                "select data from mappe where id = :id for update",
                                mapOf("id" to UUID.fromString(mappeId))
                        )
                                .map { row ->
                                    row.string("data")
                                }.asSingle
                )

                val mappe = if (!json.isNullOrEmpty()) {
                    f(objectMapper().readValue<Mappe>(json, Mappe::class.java))
                } else {
                    f(null)
                }
                //language=PostgreSQL
                tx.run(
                        queryOf(
                                """
                    insert into mappe as k (id, sist_endret, data)
                    values (:id, now(), :data :: jsonb)
                    on conflict (id) do update
                    set data = :data :: jsonb,
                    sist_endret = now()
                 """, mapOf("id" to UUID.fromString(mappeId), "data" to objectMapper().writeValueAsString(mappe))
                        ).asUpdate
                )
                return@transaction mappe
            }
        }
    }


}
