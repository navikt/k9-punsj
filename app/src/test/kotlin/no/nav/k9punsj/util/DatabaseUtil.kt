package no.nav.k9punsj.util

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import no.nav.k9punsj.akjonspunkter.AksjonspunktRepository
import no.nav.k9punsj.db.config.loadFlyway
import no.nav.k9punsj.db.repository.BunkeRepository
import no.nav.k9punsj.db.repository.MappeRepository
import no.nav.k9punsj.db.repository.PersonRepository
import no.nav.k9punsj.db.repository.SøknadRepository
import no.nav.k9punsj.journalpost.JournalpostRepository
import javax.sql.DataSource

class DatabaseUtil {

    companion object {
        internal val dataSource: DataSource = EmbeddedPostgres.start().postgresDatabase.also { dataSource ->
            val flyway = loadFlyway(dataSource)
            flyway.clean()
            flyway.migrate()
        }

        fun getMappeRepo(): MappeRepository {
            return MappeRepository(dataSource)
        }

        fun getSøknadRepo(): SøknadRepository {
            return SøknadRepository(dataSource)
        }

        fun getPersonRepo(): PersonRepository {
            return PersonRepository(dataSource)
        }

        fun getBunkeRepo(): BunkeRepository {
            return BunkeRepository(dataSource)
        }

        fun getJournalpostRepo() :JournalpostRepository {
            return JournalpostRepository(dataSource)
        }

        fun getAksjonspunktRepo() : AksjonspunktRepository {
            return AksjonspunktRepository(dataSource)
        }
    }
}

