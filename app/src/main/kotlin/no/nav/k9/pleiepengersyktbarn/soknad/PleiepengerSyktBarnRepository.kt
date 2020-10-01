package no.nav.k9.pleiepengersyktbarn.soknad

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.zaxxer.hikari.HikariDataSource
import no.nav.k9.NorskIdent
import no.nav.k9.SøknadJson
import no.nav.k9.mappe.*
import no.nav.k9.mappe.getFirstNorskIdent
import no.nav.k9.søknad.JsonUtils
import org.springframework.stereotype.Repository
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.ZonedDateTime
import java.util.*

/* UNDER ARBEID */

typealias PleiepengerSyktBarnEntities = MutableList<PleiepengerSyktBarnEntity>;

@Repository
class PleiepengerSyktBarnRepository(private val dataSource: HikariDataSource) {

    private fun prepareAndExecuteQuery(query: String): ResultSet {
        val connection = dataSource.getConnection();
        val preparedStatement = connection.prepareStatement(query);
        val result = preparedStatement.executeQuery();
        return result;
    }

    private fun prepareAndExecuteUpdate(query: String) {
        val connection = dataSource.getConnection();
        val preparedStatement = connection.prepareStatement(query);
        preparedStatement.executeUpdate();
    }

    private fun buildEntityFromResultSet(resultSet: ResultSet): PleiepengerSyktBarnEntity {
        return PleiepengerSyktBarnEntity(
                id_soknad = resultSet.getInt("id_soknad"),
                id_mappe = UUID.fromString(resultSet.getString("id_mappe")),
                norsk_ident = resultSet.getString("norsk_ident"),
                sist_endret = resultSet.getTimestamp("sist_endret"),
                barn_norsk_ident = resultSet.getString("barn_norsk_ident"),
                barn_fodselsdato = resultSet.getDate("barn_fodselsdato"),
                soknad = resultSet.getString("soknad")
        );
    }

    private fun getListOfEntitiesFromResultSet(resultSet: ResultSet): PleiepengerSyktBarnEntities {
        val entities: PleiepengerSyktBarnEntities = emptyArray<PleiepengerSyktBarnEntity>().toMutableList();
        while (resultSet.next()) {
            entities.add(this.buildEntityFromResultSet(resultSet));
        }
        return entities;
    }

    private fun groupEntitiesByMappeid(entities: PleiepengerSyktBarnEntities): MutableMap<UUID, PleiepengerSyktBarnEntities> {
        val mapper: MutableMap<UUID, PleiepengerSyktBarnEntities> = emptyMap<UUID, PleiepengerSyktBarnEntities>().toMutableMap();
        entities.forEach {
            if (mapper.keys.contains(it.id_mappe)) {
                mapper[it.id_mappe]!!.add(it);
            } else {
                mapper.put(it.id_mappe, arrayListOf(it));
            }
        }
        return mapper;
    }

    private fun convertGroupedEntitiesToMappe(entititiesGroupedByMappeid: MutableMap<UUID, PleiepengerSyktBarnEntities>): List<Mappe> {
        return entititiesGroupedByMappeid.map {
            val personer: MutableMap<NorskIdent, Person> = mutableMapOf();
            it.value.forEach {
                personer.put(it.norsk_ident, Person(
                        soeknad = ObjectMapper().readValue(it.soknad ?: "{}"),
                        innsendinger = mutableSetOf()
                ));
            }
            Mappe(
                    mappeId = it.key.toString(),
                    person = personer,
                    søknadType = "pleiepenger-sykt-barn"
            );
        }
    }

    fun oppretteSoknad(mappe: Mappe) {
        val fnr1 = mappe.getFirstNorskIdent();
        val person1 = mappe.getFirstPerson();
        val sistEndret = Timestamp.valueOf(ZonedDateTime.now().toLocalDateTime())
        val soknad = JsonUtils.toString(person1?.soeknad);
        val query =
                "insert into soknader (id_mappe, norsk_ident, sist_endret, soknad)\n" +
                "values (\n" +
                "   '${mappe.mappeId}',\n" +
                "   '$fnr1',\n" +
                "   '$sistEndret',\n" +
                "   '$soknad'\n" +
                ")"
        prepareAndExecuteUpdate(query);
    }

    fun hentAlleSoknader(): List<Mappe> {
        val query = "select * from soknader"
        val result = prepareAndExecuteQuery(query);
        val entities = getListOfEntitiesFromResultSet(result);
        val mapper = groupEntitiesByMappeid(entities);
        return convertGroupedEntitiesToMappe(mapper);
    }

    fun finneSoknad(mappeId: MappeId, fnr: NorskIdent): SøknadJson {
        val query = "select * from soknader where id_mappe = '$mappeId' and norsk_ident = '$fnr'"
        val result = prepareAndExecuteQuery(query);
        val soknad = getListOfEntitiesFromResultSet(result).first().soknad
        return ObjectMapper().readValue(soknad ?: "{}")
    }

    fun finneMappe(mappeId: MappeId): Mappe? {
        val query = "select * from soknader where id_mappe = '$mappeId'"
        val result = prepareAndExecuteQuery(query);
        val entities = getListOfEntitiesFromResultSet(result);
        val mapper = groupEntitiesByMappeid(entities);
        val convertedMapper = convertGroupedEntitiesToMappe(mapper);
        return if (convertedMapper.isEmpty()) null else convertedMapper.first();
    }

    fun oppdatereSoknad(mappe: Mappe) {
        val sistEndret = Timestamp.valueOf(ZonedDateTime.now().toLocalDateTime());
        val soknad = JsonUtils.toString(mappe.getFirstPerson()?.soeknad);
        val query =
                "update soknader set\n" +
                "   sist_endret = '${sistEndret}',\n" +
                "   soknad = '$soknad'\n" +
                "where id_mappe = '${mappe.mappeId}' and norsk_ident = '${mappe.getFirstNorskIdent()}'";
        prepareAndExecuteUpdate(query);
    }

    fun sletteMappe(mappeId: MappeId) {
        val query = "delete from soknader where id_mappe = '$mappeId'"
        prepareAndExecuteUpdate(query)
    }
}