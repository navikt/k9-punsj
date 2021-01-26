package no.nav.k9.person

import kotliquery.*
import no.nav.k9.sak.typer.AktørId
import no.nav.k9.sak.typer.PersonIdent
import org.springframework.stereotype.Repository
import java.util.*
import javax.sql.DataSource

typealias personId = UUID

@Repository
class PersonRepository(private val dataSource: DataSource) {

    suspend fun hentPersonVedPersonIdent(personIdent: PersonIdent): Person? {
        val nameQuery = "select person_id, aktoer_ident, person_ident from personer where person_ident = ?"

        return using(sessionOf(dataSource)) {
            it.transaction { tx ->
                //language=PostgreSQL
                tx.single(
                    queryOf(nameQuery, personIdent.ident), toPerson
                )
            }
        }
    }

    suspend fun hentPersonVedPersonId(personId: PersonId): Person? {
        val nameQuery = "select person_id, aktoer_ident, person_ident from personer where person_id = ?"

        return using(sessionOf(dataSource)) {
            it.transaction { tx ->
                //language=PostgreSQL
                tx.single(
                    queryOf(nameQuery, personId), toPerson
                )
            }
        }
    }

    suspend fun hentPersonVedAktørId(aktørId: AktørId): Person? {
        val nameQuery = "select person_id, aktoer_ident, person_ident from personer where aktoer_ident = ?"

        return using(sessionOf(dataSource)) {
            it.transaction { tx ->
                //language=PostgreSQL
                tx.single(
                    queryOf(nameQuery, aktørId.id), toPerson
                )
            }
        }
    }

    private val toPerson: (Row) -> Person = { row ->
        Person(row.string("person_id"), PersonIdent(row.string("aktoer_ident")), AktørId(row.string("person_ident")))
    }
}
