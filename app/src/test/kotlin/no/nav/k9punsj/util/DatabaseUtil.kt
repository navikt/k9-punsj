package no.nav.k9punsj.util

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import no.nav.k9punsj.db.config.runMigration
import no.nav.k9punsj.db.repository.BunkeRepository
import no.nav.k9punsj.db.repository.MappeRepository
import no.nav.k9punsj.db.repository.PersonRepository
import no.nav.k9punsj.db.repository.SøknadRepository
import javax.sql.DataSource

class DatabaseUtil {

    companion object {
        private var dataSource: DataSource? = null

        private fun getDataSource(): DataSource {
            if (dataSource != null) {
                return dataSource!!
            }
            val pg = EmbeddedPostgres.start()
            val postgresDatabase = pg.postgresDatabase
            runMigration(postgresDatabase)
            dataSource = postgresDatabase
            return dataSource!!
        }

        fun getMappeRepo(): MappeRepository {
            return MappeRepository(getDataSource())
        }

        fun getSøknadRepo(): SøknadRepository {
            return SøknadRepository(getDataSource())
        }

        fun getPersonRepo(): PersonRepository {
            return PersonRepository(getDataSource())
        }

        fun getBunkeRepo(): BunkeRepository {
            return BunkeRepository(getDataSource())
        }
    }
}

