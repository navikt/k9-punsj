package no.nav.k9.pleiepengersyktbarn.soknad

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import kotlinx.coroutines.runBlocking
import no.nav.k9.db.runMigration
import no.nav.k9.mappe.Mappe
import no.nav.k9.mappe.MappeRepository
import no.nav.k9.mappe.Person
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*

@ExtendWith(SpringExtension::class)
internal class MappeRepositoryTest {

    @Test
    internal fun HentAlleMapperSomInneholderEnNorskIdent(): Unit = runBlocking {
        val pg = EmbeddedPostgres.start()
        val dataSource = pg.postgresDatabase
        runMigration(dataSource)

        val repo = MappeRepository(dataSource = dataSource)
        val mappeId = UUID.randomUUID().toString()

        val m = Mappe(mappeId = mappeId,
                søknadType = "Omsorgspenger",
                person = hashMapOf("24073125894" to Person(mutableSetOf("200"), mutableMapOf())))

        repo.oppretteMappe(m)
        val mappe = repo.finneMappe(mappeId)
        assertThat(mappe).isNotNull
    }
}