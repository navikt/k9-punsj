package no.nav.k9punsj.domenetjenester

import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.db.datamodell.FagsakYtelseType
import no.nav.k9punsj.rest.eksternt.pdl.IdentPdl
import no.nav.k9punsj.rest.eksternt.pdl.PdlResponse
import no.nav.k9punsj.rest.eksternt.pdl.PdlService
import no.nav.k9punsj.rest.web.Innsending
import no.nav.k9punsj.rest.web.JournalpostInnhold
import no.nav.k9punsj.rest.web.SøknadJson
import no.nav.k9punsj.rest.web.dto.NorskIdentDto
import no.nav.k9punsj.util.DatabaseUtil
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.junit.jupiter.SpringExtension


@ExtendWith(SpringExtension::class, MockKExtension::class)
internal class MappeServiceTest {

    @MockBean
    private lateinit var pdlService: PdlService

    @Test
    fun `Skal teste`() : Unit = runBlocking {
        val mappeRepo = DatabaseUtil.getMappeRepo()
        val bunkeRepo = DatabaseUtil.getBunkeRepo()
        val søknadRepository = DatabaseUtil.getSøknadRepo()
        val personRepo = DatabaseUtil.getPersonRepo()
        val mappeService = MappeService(mappeRepo,
            søknadRepository,
            bunkeRepo,
            PersonService(personRepo, pdlService))
        val innsending = lagInnsending("01010050053", "999")
        val dummyAktørId = "1000000000000"
        val identer = IdentPdl.Data.HentIdenter.Identer(gruppe = "AKTORID", false, dummyAktørId)
        val identPdl = IdentPdl(IdentPdl.Data(IdentPdl.Data.HentIdenter(identer = listOf(identer))), null)

        Mockito.`when`(pdlService.identifikator(Mockito.anyString())).thenReturn(PdlResponse(false, identPdl))

        val førsteInnsending = mappeService.førsteInnsending(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, innsending)

        val test : String

    }

    private fun lagInnsending(
        personnummer: NorskIdentDto,
        journalpostId: String,
        søknad: SøknadJson = mutableMapOf(),
    ): Innsending {
        val person = JournalpostInnhold(journalpostId = journalpostId, soeknad = søknad)
        val personer = mutableMapOf<String, JournalpostInnhold<SøknadJson>>()
        personer[personnummer] = person

        return Innsending(personer)
    }
}
