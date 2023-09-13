package no.nav.k9punsj.domenetjenester

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import no.nav.k9.kodeverk.dokument.Brevkode
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.personopplysninger.Barn
import no.nav.k9.søknad.felles.personopplysninger.Søker
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.ytelse.Ytelse
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn
import no.nav.k9punsj.domenetjenester.repository.SøknadRepository
import no.nav.k9punsj.felles.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.k9punsj.felles.JournalpostId.Companion.somJournalpostId
import no.nav.k9punsj.felles.dto.SøknadEntitet
import no.nav.k9punsj.innsending.InnsendingClient
import no.nav.k9punsj.integrasjoner.dokarkiv.DokarkivGateway
import no.nav.k9punsj.integrasjoner.dokarkiv.FerdigstillJournalpost
import no.nav.k9punsj.integrasjoner.dokarkiv.JoarkTyper
import no.nav.k9punsj.integrasjoner.dokarkiv.JoarkTyper.JournalpostStatus.Companion.somJournalpostStatus
import no.nav.k9punsj.integrasjoner.dokarkiv.JoarkTyper.JournalpostType.Companion.somJournalpostType
import no.nav.k9punsj.integrasjoner.dokarkiv.JournalPostResponse
import no.nav.k9punsj.integrasjoner.dokarkiv.SafDtos
import no.nav.k9punsj.integrasjoner.dokarkiv.SafGateway
import no.nav.k9punsj.integrasjoner.k9sak.K9SakService
import no.nav.k9punsj.integrasjoner.pdl.PdlService
import no.nav.k9punsj.integrasjoner.pdl.Personopplysninger
import no.nav.k9punsj.integrasjoner.sak.SakClient
import no.nav.k9punsj.journalpost.JournalpostService
import no.nav.k9punsj.journalpost.dto.PunsjJournalpost
import no.nav.k9punsj.metrikker.SøknadMetrikkService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.time.LocalDateTime
import java.util.*

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

    @MockK(relaxUnitFun = true)
    private lateinit var sakClient: SakClient

    @MockK(relaxUnitFun = true)
    private lateinit var dokarkivGateway: DokarkivGateway

    @MockK(relaxUnitFun = true)
    private lateinit var k9SakService: K9SakService

    @MockK(relaxUnitFun = true)
    private lateinit var pdlService: PdlService

    @MockK
    private lateinit var mockSafGateway: SafGateway

    private lateinit var soknadService: SoknadService

    @BeforeAll
    fun setup() {
        MockKAnnotations.init(this)
        coEvery { mockJournalpostService.kanSendeInn(listOf(feilregistrertJournalpost.journalpostId)) }.returns(true)
        coEvery { mockSøknadRepository.hentSøknad(any()) }.returns(
            SøknadEntitet(
                søknadId = "1",
                bunkeId = "1",
                søkerId = "1",
                endret_av = "1"
            )
        )
        coEvery { mockJournalpostService.hent(any()) }.returns(PunsjJournalpost(UUID.randomUUID(), "1", aktørId = "1"))
        soknadService = SoknadService(
            journalpostService = mockJournalpostService,
            søknadRepository = mockSøknadRepository,
            søknadMetrikkService = mockSøknadMetrikkService,
            safGateway = mockSafGateway,
            k9SakService = k9SakService,
            sakClient = sakClient,
            pdlService = pdlService,
            dokarkivGateway = dokarkivGateway
        )
    }

    private val feilregistrertJournalpost = SafDtos.Journalpost(
        journalpostId = "525115311",
        tittel = "omsorgspengerutbetaling",
        tema = "OMS",
        journalposttype = "N",
        journalstatus = "FEILREGISTRERT",
        bruker = SafDtos.Bruker(
            id = "2351670926708",
            type = "AKTOERID"
        ),
        sak = SafDtos.Sak(
            sakstype = SafDtos.Sakstype.FAGSAK,
            fagsakId = "AB123",
            fagsaksystem = "k9",
            tema = SafDtos.Tema.OMS
        ),
        avsender = null,
        avsenderMottaker = SafDtos.AvsenderMottaker(id = null, type = null, null),
        dokumenter = listOf(
            SafDtos.Dokument(
                dokumentInfoId = "549312456",
                brevkode = "K9_PUNSJ_NOTAT",
                tittel = "tittel på dokument",
                dokumentvarianter = mutableListOf(
                    SafDtos.DokumentVariant(variantformat = "ORIGINAL", saksbehandlerHarTilgang = true),
                    SafDtos.DokumentVariant(variantformat = "ARKIV", saksbehandlerHarTilgang = true)
                )
            )
        ),
        relevanteDatoer = listOf(
            SafDtos.RelevantDato(
                dato = LocalDateTime.parse("2022-07-01T13:32:05"),
                datotype = SafDtos.Datotype.DATO_DOKUMENT
            ),
            SafDtos.RelevantDato(
                dato = LocalDateTime.parse("2022-07-01T13:32:05"),
                datotype = SafDtos.Datotype.DATO_JOURNALFOERT
            )
        )
    )

    @Test
    fun `Feilregistrert journalpost returnerar conflict fra innsending i soknadservice`() = runBlocking {
        coEvery { mockSafGateway.hentJournalposter(any()) }.returns(listOf(feilregistrertJournalpost))
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
        val riktigJournalpost = feilregistrertJournalpost.copy(
            journalstatus = SafDtos.Journalstatus.MOTTATT.toString()
        )
        coEvery { mockSafGateway.hentJournalposter(any()) }.returns(listOf(riktigJournalpost))
        coEvery { mockSafGateway.hentFerdigstillJournalpost(any()) }.returns(
            FerdigstillJournalpost(
                journalpostId = "525115311".somJournalpostId(),
                status = "F".somJournalpostStatus(),
                type = "I".somJournalpostType(),
                avsendernavn = null,
                tittel = null,
                dokumenter = emptySet(),
                bruker = null,
                sak = FerdigstillJournalpost.Sak(null, null, null)
            )
        )
        coEvery { k9SakService.hentEllerOpprettSaksnummer(any()) }.returns(Pair("ABC123", null))
        coEvery { pdlService.hentPersonopplysninger(any()) }.returns(
            setOf(
                Personopplysninger(
                    identitetsnummer = "21040076619",
                    fødselsdato = LocalDateTime.now().toLocalDate(),
                    fornavn = "fornavn",
                    mellomnavn = "mellomnavn",
                    etternavn = "etternavn",
                    gradering = Personopplysninger.Gradering.UGRADERT
                )
            )
        )
        coEvery { dokarkivGateway.ferdigstillJournalpost(any(), any()) }.returns(ResponseEntity(HttpStatus.OK))
        coEvery { dokarkivGateway.opprettOgFerdigstillJournalpost(any()) }.returns(JournalPostResponse(("123123123")))


        val søknad = Søknad()
            .medSøknadId("1")
            .medYtelse(
                PleiepengerSyktBarn()
                    .medBarn(
                        Barn()
                            .medNorskIdentitetsnummer(NorskIdentitetsnummer.of("20032390359"))
                    )
            )
            .medSøker(Søker(NorskIdentitetsnummer.of("21040076619")))

        val result = soknadService.sendSøknad(
            søknad = søknad,
            brevkode = Brevkode.PLEIEPENGER_BARN_SOKNAD,
            journalpostIder = mutableSetOf("525115311")
        )

        Assertions.assertNull(result) // Forventet return om allt går bra er null
    }
}
