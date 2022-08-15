package no.nav.k9punsj.domenetjenester

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import no.nav.k9.kodeverk.dokument.Brevkode
import no.nav.k9.søknad.Søknad
import no.nav.k9punsj.domenetjenester.repository.SøknadRepository
import no.nav.k9punsj.felles.dto.SøknadEntitet
import no.nav.k9punsj.innsending.InnsendingClient
import no.nav.k9punsj.integrasjoner.dokarkiv.SafDtos
import no.nav.k9punsj.integrasjoner.dokarkiv.SafGateway
import no.nav.k9punsj.journalpost.JournalpostService
import no.nav.k9punsj.journalpost.PunsjJournalpost
import no.nav.k9punsj.metrikker.SøknadMetrikkService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import java.time.LocalDateTime
import java.util.UUID

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SoknadServiceTest {

    @MockK(relaxUnitFun = true)
    private lateinit var mockSøknadRepository: SøknadRepository
    @MockK(relaxUnitFun = true)
    private lateinit var mockInnsendingClient: InnsendingClient
    @MockK(relaxUnitFun = true)
    private lateinit var mockSøknadMetrikkService: SøknadMetrikkService
    @MockK(relaxUnitFun = true)
    private lateinit var mockJournalpostService: JournalpostService

    @MockK
    private lateinit var mockSafGateway: SafGateway

    private lateinit var soknadService: SoknadService

    @BeforeAll
    fun setup() {
        MockKAnnotations.init(this)
        coEvery { mockJournalpostService.kanSendeInn(listOf(feilregistrertJournalpost.journalpostId)) }.returns(true)
        coEvery { mockSøknadRepository.hentSøknad(any()) }.returns(SøknadEntitet(søknadId = "1", bunkeId = "1", søkerId = "1", endret_av = "1"))
        coEvery { mockJournalpostService.hent(any()) }.returns(PunsjJournalpost(UUID.randomUUID(), "1", aktørId = "1"))
        soknadService = SoknadService(mockJournalpostService, mockSøknadRepository, mockInnsendingClient, mockSøknadMetrikkService, mockSafGateway)
    }

    private val feilregistrertJournalpost = SafDtos.Journalpost(
        journalpostId = "525115311",
        tema = "OMS", journalposttype = "N", journalstatus = "FEILREGISTRERT",
        bruker = SafDtos.Bruker(id = "2351670926708", type = "AKTOERID"),
        avsender = null,
        avsenderMottaker = SafDtos.AvsenderMottaker(id = null, type = null),
        dokumenter = listOf(SafDtos.Dokument(
            dokumentInfoId = "549312456", brevkode = "K9_PUNSJ_NOTAT",
            dokumentvarianter = mutableListOf(
                SafDtos.DokumentVariant(variantformat = "ORIGINAL", saksbehandlerHarTilgang = true),
                SafDtos.DokumentVariant(variantformat = "ARKIV", saksbehandlerHarTilgang = true)
            )
        )),
        relevanteDatoer = listOf(
            SafDtos.RelevantDato(dato = LocalDateTime.parse("2022-07-01T13:32:05"), datotype = SafDtos.Datotype.DATO_DOKUMENT),
            SafDtos.RelevantDato(dato = LocalDateTime.parse("2022-07-01T13:32:05"), datotype = SafDtos.Datotype.DATO_JOURNALFOERT)
        )
    )

    @Test
    fun `Feilregistrert journalpost returnerar conflict fra innsending i soknadservice`() = runBlocking {
        coEvery { mockSafGateway.hentJournalposter(any())}.returns(listOf(feilregistrertJournalpost))
        val result = soknadService.sendSøknad(
            søknad = Søknad().medSøknadId("1"),
            brevkode = Brevkode.PLEIEPENGER_BARN_SOKNAD,
            journalpostIder = mutableSetOf("525115311")
        )

        Assertions.assertNotNull(result)
        Assertions.assertEquals(HttpStatus.CONFLICT, result!!.first)
    }

    @Test
    fun `Innsendt journalpost uten feil returnerer null`() = runBlocking {
        val riktigJournalpost = feilregistrertJournalpost.copy(journalstatus = SafDtos.Journalstatus.MOTTATT.toString())
        coEvery { mockSafGateway.hentJournalposter(any())}.returns(listOf(riktigJournalpost))

        val result = soknadService.sendSøknad(
            søknad = Søknad().medSøknadId("1"),
            brevkode = Brevkode.PLEIEPENGER_BARN_SOKNAD,
            journalpostIder = mutableSetOf("525115311")
        )

        Assertions.assertNull(result) // Forventet return om allt går bra er null
    }
}
