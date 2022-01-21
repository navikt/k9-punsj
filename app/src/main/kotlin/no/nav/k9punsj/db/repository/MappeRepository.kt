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
   companion object {
       const val MAPPE_TABLE = "mappe"
   }

    suspend fun hent(personIder: Set<PersonId>): List<Mappe> {
        return using(sessionOf(dataSource)) {
            it.transaction { tx ->
                //language=PostgreSQL
                tx.run(
                    queryOf(
                        "select data from $MAPPE_TABLE where (data -> 'personInfo') ??| array['${personIder.joinToString("','")}']",
                    )
                        .map { row ->
                            objectMapper().readValue<Mappe>(row.string("data"))
                        }.asList
                )
            }
        }
    }

    suspend fun lagre(mappeId: MappeId, f: (Mappe?) -> Mappe): Mappe {
        return using(sessionOf(dataSource)) {
            return@using it.transaction { tx ->
                val json = tx.run(
                    queryOf(
                        "select data from $MAPPE_TABLE where id = :id for update",
                        mapOf("id" to UUID.fromString(mappeId))
                    )
                        .map { row ->
                            row.string("data")
                        }.asSingle
                )

                val mappe = if (!json.isNullOrEmpty()) {
                    f(objectMapper().readValue(json, Mappe::class.java))
                } else {
                    f(null)
                }
                //language=PostgreSQL
                tx.run(
                    queryOf(
                        """
                    insert into $MAPPE_TABLE as k (id, endret_tid, data)
                    values (:id, now(), :data :: jsonb)
                    on conflict (id) do update
                    set data = :data :: jsonb,
                    endret_tid = now()
                 """, mapOf("id" to UUID.fromString(mappeId), "data" to objectMapper().writeValueAsString(mappe))
                    ).asUpdate
                )
                return@transaction mappe
            }
        }
    }

    suspend fun hentEierAvMappe(mappeId: MappeId): PersonId? {
        return using(sessionOf(dataSource)) {
            return@using it.transaction { tx ->
                return@transaction tx.run(
                    queryOf(
                        "select id_person from $MAPPE_TABLE where id = :mappeId",
                        mapOf("mappeId" to UUID.fromString(mappeId))
                    )
                        .map { row ->
                            row.string("id_person")
                        }.asSingle
                )
            }
        }

    }

    suspend fun opprettEllerHentMappeForPerson(personId: PersonId): MappeId {
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
                    
                 """, mapOf("id" to mappeId, "id_person" to UUID.fromString(personId))
                    ).asUpdate
                )
                return@transaction mappeId.toString()
            }
        }
    }
}
