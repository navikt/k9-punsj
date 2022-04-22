package no.nav.k9punsj.db.repository

import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9punsj.db.datamodell.Person
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.sql.DataSource

@Repository
class PersonRepository(private val dataSource: DataSource) {

    companion object {
        const val PERSON_TABLE = "person"
    }

    suspend fun hentPersonVedPersonIdent(norskIdent: String): Person? {
        val nameQuery = "select person_id, aktoer_ident, norsk_ident from $PERSON_TABLE where norsk_ident = ?"

        return using(sessionOf(dataSource)) {
            return@using it.transaction { tx ->
                //language=PostgreSQL
                return@transaction tx.single(
                    queryOf(nameQuery, norskIdent), toPerson
                )
            }
        }
    }

    suspend fun hentPersonVedPersonId(personId: String): Person? {
        val nameQuery = "select person_id, aktoer_ident, norsk_ident from $PERSON_TABLE where person_id = ?"

        return using(sessionOf(dataSource)) {
            return@using it.transaction { tx ->
                //language=PostgreSQL
                return@transaction tx.single(
                    queryOf(nameQuery, UUID.fromString(personId)), toPerson
                )
            }
        }
    }

    suspend fun lagre(norskIdent: String, aktørId: String): Person {
        val uuid = UUID.randomUUID()

        val insert = "insert into $PERSON_TABLE (person_id,  aktoer_ident, norsk_ident) values (?, ?, ?)"

        using(sessionOf(dataSource)) {
            return@using it.transaction { tx ->
                //language=PostgreSQL
                return@transaction tx.update(queryOf(insert, uuid, aktørId, norskIdent))
            }
        }

        return Person(personId = uuid.toString(), norskIdent = norskIdent, aktørId = aktørId)
    }

    private val toPerson: (Row) -> Person = { row ->
        Person(personId = row.string("person_id"), aktørId = row.string("aktoer_ident"), norskIdent = row.string("norsk_ident"))
    }
}
