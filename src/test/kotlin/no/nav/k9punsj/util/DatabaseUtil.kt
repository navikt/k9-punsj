package no.nav.k9punsj.util

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9punsj.TestProfil
import no.nav.k9punsj.akjonspunkter.AksjonspunktRepository
import no.nav.k9punsj.domenetjenester.repository.BunkeRepository
import no.nav.k9punsj.domenetjenester.repository.MappeRepository
import no.nav.k9punsj.domenetjenester.repository.PersonRepository
import no.nav.k9punsj.domenetjenester.repository.SøknadRepository
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.metrikker.JournalpostMetrikkRepository
import org.flywaydb.core.Flyway
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import javax.sql.DataSource

class DatabaseUtil {
    companion object {
        internal val embeddedPostgres = EmbeddedPostgres.start()
        internal val dataSource: DataSource = embeddedPostgres.postgresDatabase.also { dataSource ->
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
}
