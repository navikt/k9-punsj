package no.nav.k9.util

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import no.nav.k9.db.runMigration
import no.nav.k9.mappe.MappeRepository
import javax.sql.DataSource

class DatabaseUtil {

    companion object {
        fun getDataSource(): DataSource {
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
