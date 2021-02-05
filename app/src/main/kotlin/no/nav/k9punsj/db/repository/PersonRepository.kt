package no.nav.k9punsj.db.repository

import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9punsj.db.datamodell.AktørId
import no.nav.k9punsj.db.datamodell.NorskIdent
import no.nav.k9punsj.db.datamodell.Person
import no.nav.k9punsj.db.datamodell.PersonId
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

@Repository
class PersonRepository(private val dataSource: DataSource) {

    suspend fun hentPersonVedPersonIdent(norskIdent: NorskIdent): Person? {
        val nameQuery = "select person_id, aktoer_ident, norsk_ident from person where norsk_ident = ?"

        return using(sessionOf(dataSource)) {
            return@using it.transaction { tx ->
                //language=PostgreSQL
                return@transaction tx.single(
                    queryOf(nameQuery, norskIdent), toPerson
                )
            }
        }
    }

    suspend fun hentPersonVedPersonId(personId: PersonId): Person? {
        val nameQuery = "select person_id, aktoer_ident, norsk_ident from person where person_id = ?"

        return using(sessionOf(dataSource)) {
            return@using it.transaction { tx ->
                //language=PostgreSQL
                return@transaction tx.single(
                    queryOf(nameQuery, UUID.fromString(personId)), toPerson
                )
            }
        }
    }

    suspend fun hentPersonVedAktørId(aktørId: AktørId): Person? {
        val nameQuery = "select person_id, aktoer_ident, norsk_ident from person where aktoer_ident = ?"

        return using(sessionOf(dataSource)) {
            return@using it.transaction { tx ->
                //language=PostgreSQL
                return@transaction tx.single(
                    queryOf(nameQuery, aktørId), toPerson
                )
            }
        }
    }

    suspend fun lagre(norskIdent: String, aktørId: String): Person {
        val uuid = UUID.randomUUID()

        val insert = "insert into person (person_id,  aktoer_ident, norsk_ident, sist_endret) values (?, ?, ?, ?)"

        using(sessionOf(dataSource)) {
            return@using it.transaction { tx ->
                //language=PostgreSQL
                return@transaction tx.update(queryOf(insert, uuid, aktørId, norskIdent, LocalDateTime.now()))
            }
        }

        return Person(personId = uuid.toString(), norskIdent = norskIdent, aktørId = aktørId)
    }

    private val toPerson: (Row) -> Person = { row ->
        Person(personId = row.string("person_id"), aktørId = row.string("aktoer_ident"), norskIdent = row.string("norsk_ident"))
    }
}
