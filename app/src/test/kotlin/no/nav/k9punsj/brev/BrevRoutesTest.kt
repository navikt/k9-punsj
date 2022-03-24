package no.nav.k9punsj.brev

import com.fasterxml.jackson.module.kotlin.convertValue
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9.formidling.kontrakt.kodeverk.FagsakYtelseType
import no.nav.k9punsj.TestSetup
import no.nav.k9punsj.brev.BrevVisningDto
import no.nav.k9punsj.brev.DokumentbestillingDto
import no.nav.k9punsj.db.datamodell.JsonB
import no.nav.k9punsj.fordel.PunsjInnsendingType
import no.nav.k9punsj.journalpost.PunsjJournalpost
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.openapi.OasFeil
import no.nav.k9punsj.util.DatabaseUtil
import no.nav.k9punsj.util.IdGenerator
import no.nav.k9punsj.util.WebClientUtils.awaitBodyWithType
import no.nav.k9punsj.util.WebClientUtils.awaitStatusWithBody
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.BodyInserters
import java.util.UUID


@ExtendWith(SpringExtension::class, MockKExtension::class)
internal class BrevRoutesTest {

    private val client = TestSetup.client
    private val api = "api"
    private val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"


    @Test
    fun `Bestill brev og send til k9-formidling på kafka`() : Unit = runBlocking {
        val journalpostId = lagJournalpost()
        val norskIdent = "01110050053"

        val body = lagBestilling(norskIdent, journalpostId)

        val (httpStatus, oasFeil) = client.post()
            .uri { it.pathSegment(api, "brev", "bestill").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(body))
            .awaitStatusWithBody<OasFeil>()

        assertThat(oasFeil.feil).isNull()
        assertThat(httpStatus).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `Hent opp brevbestillinger`() : Unit = runBlocking {
        val journalpostId = lagJournalpost()
        val norskIdent = "01110050053"

        val body = lagBestilling(norskIdent, journalpostId)

        val (httpStatus, oasFeil) = client.post()
            .uri { it.pathSegment(api, "brev", "bestill").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(body))
            .awaitStatusWithBody<OasFeil>()

        assertThat(oasFeil.feil).isNull()
        assertThat(httpStatus).isEqualTo(HttpStatus.OK)

        val dtoByGet = client.get()
            .uri { it.pathSegment(api, "brev", "hentAlle", journalpostId).build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .awaitBodyWithType<List<BrevVisningDto>>()

        val brevVisningDto = dtoByGet[0]

        assertThat(brevVisningDto.journalpostId).isEqualTo(journalpostId)
        assertThat(brevVisningDto.sendtInnAv).isEqualTo("saksbehandler@nav.no")
    }

    @Test
    fun `Skal feile hvis man prøver å sende brev på en ferdig behandlet journalpost`() : Unit = runBlocking {
        val journalpostId = lagJournalpost()
        val norskIdent = "01110050053"
        DatabaseUtil.getJournalpostRepo().ferdig(journalpostId)

        val body = lagBestilling(norskIdent, journalpostId)

        val (httpStatus, oasFeil) = client.post()
            .uri { it.pathSegment(api, "brev", "bestill").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(body))
            .awaitStatusWithBody<OasFeil>()

        assertThat(oasFeil.feil).isEqualTo("Kan ikke bestille brev på en journalpost som er ferdig behandlet av punsj")
        assertThat(httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    private suspend fun lagJournalpost(): String {
        val journalpostId = IdGenerator.nesteId()
        val aktørId = "100000000"

        val jp =
            PunsjJournalpost(uuid = UUID.randomUUID(),
                journalpostId = journalpostId,
                aktørId = aktørId,
                type = PunsjInnsendingType.INNTEKTSMELDING_UTGÅTT.kode)
        DatabaseUtil.getJournalpostRepo().lagre(jp) {
            jp
        }
        return journalpostId
    }

    private fun lagBestilling(søker: String, journalpostId: String): JsonB {
        return objectMapper().convertValue(
            DokumentbestillingDto(
                journalpostId = journalpostId,
                soekerId = søker,
                mottaker = DokumentbestillingDto.Mottaker("ORGNR", "1231245"),
                fagsakYtelseType = FagsakYtelseType.OMSORGSPENGER,
                dokumentMal = "INNTID"
            )
        )
    }
}
