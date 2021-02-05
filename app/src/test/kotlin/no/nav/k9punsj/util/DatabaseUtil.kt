package no.nav.k9punsj.util

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import no.nav.k9punsj.db.config.runMigration
import no.nav.k9punsj.db.repository.MappeRepository
import javax.sql.DataSource

class DatabaseUtil {

    companion object {
        private fun getDataSource(): DataSource {
            val pg = EmbeddedPostgres.start()
            val dataSource = pg.postgresDatabase
            runMigration(dataSource)
            return dataSource
        }

        fun getMappeRepo(): MappeRepository {
            return MappeRepository(getDataSource())
        }
    }
}
