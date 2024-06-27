package no.nav.k9punsj.journalpost.postmottak

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import kotlinx.coroutines.runBlocking
import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.felles.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.k9punsj.fordel.K9FordelType
import no.nav.k9punsj.integrasjoner.dokarkiv.DokarkivGateway
import no.nav.k9punsj.integrasjoner.dokarkiv.SafDtos
import no.nav.k9punsj.integrasjoner.dokarkiv.SafGateway
import no.nav.k9punsj.integrasjoner.k9sak.K9SakService
import no.nav.k9punsj.integrasjoner.k9sak.dto.Fagsak
import no.nav.k9punsj.integrasjoner.pdl.PdlService
import no.nav.k9punsj.journalpost.JournalpostService
import no.nav.k9punsj.journalpost.dto.PunsjJournalpost
import no.nav.k9punsj.util.opprettKafkaStringConsumer
import org.apache.kafka.clients.consumer.Consumer
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.json.JSONObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.kafka.test.utils.KafkaTestUtils
import java.time.Duration
import java.util.*

class PostMottakServiceTest : AbstractContainerBaseTest() {

    @MockkBean
    private lateinit var pdlService: PdlService

    @MockkBean
    private lateinit var k9SakService: K9SakService

    @MockkBean
    private lateinit var personService: PersonService

    @MockkBean
    private lateinit var safGateway: SafGateway

    @MockkBean
    private lateinit var dokarkivGateway: DokarkivGateway

    @Autowired
    private lateinit var journalpostService: JournalpostService

    @Autowired
    private lateinit var postMottakService: PostMottakService

    @Value("\${no.nav.kafka.k9_punsj_til_los.topic}")
    private lateinit var k9LosTopic: String

    lateinit var k9LosKafkaConsumer: Consumer<String, String>

    @BeforeEach
    fun setUp() {
        k9LosKafkaConsumer = embeddedKafkaBroker.opprettKafkaStringConsumer(
            groupId = "k9-punsj",
            topics = listOf(k9LosTopic)
        )
    }

    @Test
    fun `Forventer at journalføringstidspunkt sendes tl LOS`(): Unit = runBlocking {
        val brukerIdent = "12345678900"
        coEvery { pdlService.aktørIdFor(brukerIdent) } returns brukerIdent

        val pleietrengendeIdent = "10987654321"
        coEvery { personService.finnAktørId(pleietrengendeIdent) } returns pleietrengendeIdent

        val journalpostId = "123"
        val saksnummer = "ABC123"

        coEvery { k9SakService.hentFagsaker(brukerIdent) } returns Pair(
            setOf(
                Fagsak(
                    saksnummer = saksnummer,
                    sakstype = FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
                    pleietrengendeAktorId = pleietrengendeIdent,
                    relatertPersonAktørId = null,
                    gyldigPeriode = null

                )
            ), null
        )

        coEvery { safGateway.hentJournalpostInfo(journalpostId) } returns opprettSafJournalpost(
            journalpostId,
            brukerIdent,
            saksnummer
        )

        coEvery { safGateway.hentDataFraSaf(journalpostId) } returns JSONObject() // ikke viktig for denne testen

        val mottattJournalpost = JournalpostMottaksHaandteringDto(
            journalpostId = journalpostId,
            brukerIdent = brukerIdent,
            pleietrengendeIdent = pleietrengendeIdent,
            relatertPersonIdent = null,
            fagsakYtelseTypeKode = "PSB",
            saksnummer = saksnummer,
        )

        coEvery {
            dokarkivGateway.oppdaterJournalpostDataOgFerdigstill(
                any(),
                journalpostId,
                brukerIdent.somIdentitetsnummer(),
                any(),
                any()
            )
        } returns Pair(HttpStatus.OK, "")

        val punsjJournalpost = opprettPunsjJournalpost(
            uuid = UUID.randomUUID(),
            brukerIdent = brukerIdent,
            journalpostId = journalpostId,
            k9FordelType = K9FordelType.PAPIRSØKNAD
        )

        journalpostService.opprettJournalpost(punsjJournalpost)

        // Forventer at journalføringstidspunkt sendes til LOS
        postMottakService.klassifiserOgJournalfør(mottattJournalpost)
        val records = KafkaTestUtils.getSingleRecord(k9LosKafkaConsumer, k9LosTopic, Duration.ofSeconds(10))
        assertThat(records).isNotNull()
        records?.let {
            val json = JSONObject(it.value())
            val journalførtTidspunkt = json.getString("journalførtTidspunkt")
            assertThat(journalførtTidspunkt).isNotBlank()
        }
    }

    private fun opprettSafJournalpost(
        journalpostId: String,
        brukerIdent: String,
        saksnummer: String,
    ) = SafDtos.Journalpost(
        journalpostId = journalpostId,
        tema = "OMS",
        tittel = "Søknad om pleipenger til sykt barn",
        journalposttype = SafDtos.JournalpostType.I.name,
        journalstatus = SafDtos.Journalstatus.MOTTATT.name,
        bruker = SafDtos.Bruker(
            id = brukerIdent,
            type = "FNR"
        ),
        sak = SafDtos.Sak(
            sakstype = SafDtos.Sakstype.FAGSAK,
            fagsakId = saksnummer,
            fagsaksystem = "K9",
            tema = SafDtos.Tema.OMS.name
        ),
        avsender = null,
        avsenderMottaker = null,
        dokumenter = listOf(),
        relevanteDatoer = listOf(),
        tilleggsopplysninger = listOf()
    )

    private fun opprettPunsjJournalpost(
        uuid: UUID,
        brukerIdent: String,
        journalpostId: String,
        k9FordelType: K9FordelType,
    ) = PunsjJournalpost(
        uuid = uuid,
        aktørId = brukerIdent,
        journalpostId = journalpostId,
        type = k9FordelType.kode,
        skalTilK9 = null,
        mottattDato = null,
        journalførtTidspunkt = null,
        gosysoppgaveId = null,
        ytelse = null,
        payload = null,
        behandlingsAar = null,
        fordelStatusType = null
    )
}
