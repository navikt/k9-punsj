package no.nav.k9punsj.util

import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9punsj.akjonspunkter.AksjonspunktRepository
import no.nav.k9punsj.configuration.DbConfiguration
import no.nav.k9punsj.domenetjenester.repository.BunkeRepository
import no.nav.k9punsj.domenetjenester.repository.MappeRepository
import no.nav.k9punsj.domenetjenester.repository.PersonRepository
import no.nav.k9punsj.domenetjenester.repository.SøknadRepository
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.metrikker.JournalpostMetrikkRepository
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer
import javax.sql.DataSource

object DatabaseUtil {

    internal var postgresContainer: PostgreSQLContainer<*>? = null

    init {
        postgresContainer = PostgreSQLContainer("postgres:12")
            .withDatabaseName("k9_punsj")
            .withUsername("k9_punsj")
            .withPassword("k9_punsj").also { it.start() }
    }

    internal val dataSource: DataSource = HikariDataSource(
        DbConfiguration().hikariConfig(
            mapOf(
                "DATABASE_HOST" to "127.0.0.1",
                "DATABASE_PORT" to postgresContainer?.getFirstMappedPort().toString(),
                "DATABASE_DATABASE" to "k9_punsj",
                "DATABASE_USERNAME" to "k9_punsj",
                "DATABASE_PASSWORD" to "k9_punsj"
            )
        )
    ).also { dataSource ->
        val flyway = Flyway.configure()
            .cleanDisabled(false)
            .locations("migreringer/")
            .dataSource(dataSource)
            .load()!!
        flyway.clean()
        flyway.migrate()
    }

    fun getMappeRepo(): MappeRepository = MappeRepository(dataSource)
    fun cleanMappeRepo() = cleanTable(MappeRepository.MAPPE_TABLE)

    fun getSøknadRepo(): SøknadRepository = SøknadRepository(dataSource)
    fun cleanSøknadRepo() = cleanTable(SøknadRepository.SØKNAD_TABLE)

    fun getPersonRepo(): PersonRepository = PersonRepository(dataSource)
    fun cleanPersonRepo() = cleanTable(PersonRepository.PERSON_TABLE)

    fun getBunkeRepo(): BunkeRepository = BunkeRepository(dataSource)
    fun cleanBunkeRepo() = cleanTable(BunkeRepository.BUNKE_TABLE)

    fun getJournalpostRepo(): JournalpostRepository = JournalpostRepository(dataSource)
    fun journalpostMetrikkRepository(): JournalpostMetrikkRepository = JournalpostMetrikkRepository(dataSource)
    fun cleanJournalpostRepo() = cleanTable(JournalpostRepository.JOURNALPOST_TABLE)

    fun getAksjonspunktRepo(): AksjonspunktRepository = AksjonspunktRepository(dataSource)
    fun cleanAksjonspunktRepo() = cleanTable(AksjonspunktRepository.AKSJONSPUNKT_TABLE)

    fun cleanDB() {
        cleanSøknadRepo()
        cleanBunkeRepo()
        cleanMappeRepo()
        cleanPersonRepo()
        cleanAksjonspunktRepo()
        cleanJournalpostRepo()
    }

    private fun cleanTable(tableName: String) {
        using(sessionOf(dataSource)) {
            //language=PostgreSQL
            it.transaction { tx -> tx.run(queryOf("DELETE FROM $tableName;").asExecute) }
        }
    }
}
