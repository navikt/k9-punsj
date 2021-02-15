package no.nav.k9punsj

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import no.nav.k9punsj.db.config.runMigration
import no.nav.k9punsj.db.datamodell.NorskIdent
import no.nav.k9punsj.kafka.HendelseProducer
import no.nav.k9punsj.rest.eksternt.k9sak.K9SakService
import no.nav.k9punsj.rest.eksternt.pdl.IdentPdl
import no.nav.k9punsj.rest.eksternt.pdl.PdlResponse
import no.nav.k9punsj.rest.eksternt.pdl.PdlService
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadDto
import no.nav.k9punsj.rest.web.dto.SaksnummerDto
import no.nav.k9punsj.util.LesFraFilUtil
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import javax.sql.DataSource

@TestConfiguration
@Profile("test")
class TestContext {

    @Bean
    fun hendelseProducerBean() = hendelseProducerMock
    val hendelseProducerMock: HendelseProducer = object: HendelseProducer {
        override fun send(topicName: String, søknadString: String, søknadId: String) {

        }
    }

    @Bean
    fun k9ServiceBean() = k9ServiceMock
    val k9ServiceMock: K9SakService = object: K9SakService{
        override suspend fun hentSisteMottattePsbSøknad(norskIdent: NorskIdent): PleiepengerSøknadDto? {
            return LesFraFilUtil.hentKomplettSøknad()
        }

        override suspend fun opprettEllerHentFagsakNummer(): SaksnummerDto {
            TODO("Not yet implemented")
        }
    }

    @Bean
    fun pdlServiceBean() = pdlServiceMock
    val pdlServiceMock: PdlService = object: PdlService{
        val dummyFnr = "11111111111"
        val dummyAktørId = "1000000000000"

        override suspend fun identifikator(fnummer: String): PdlResponse? {
            val identer = IdentPdl.Data.HentIdenter.Identer(gruppe = "AKTORID", false, dummyAktørId)
            val identPdl = IdentPdl(IdentPdl.Data(IdentPdl.Data.HentIdenter(identer = listOf(identer))), null)

            return PdlResponse(false, identPdl)
        }

        override suspend fun identifikatorMedAktørId(aktørId: String): PdlResponse? {
            val identer = IdentPdl.Data.HentIdenter.Identer(gruppe = "FOLKEREGISTERIDENT", false, dummyFnr)
            val identPdl = IdentPdl(IdentPdl.Data(IdentPdl.Data.HentIdenter(identer = listOf(identer))), null)

            return PdlResponse(false, identPdl)
        }
    }


    @Bean
    fun databaseInitializer(): DataSource {
        val pg = EmbeddedPostgres.builder().setPort(5433).start()
        val dataSource = pg.postgresDatabase
        runMigration(dataSource)
        return dataSource
    }
}
