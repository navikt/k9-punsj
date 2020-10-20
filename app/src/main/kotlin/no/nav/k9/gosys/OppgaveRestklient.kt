package no.nav.k9.gosys

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9.journalpost.SafDtos
import no.nav.k9.journalpost.SafGateway
import no.nav.k9.objectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import java.net.URI
import java.time.LocalDate

@Service
class OppgaveRestklient(
        @Value("\${no.nav.saf.base_url}") safBaseUrl: URI
) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(SafGateway::class.java)
        private const val VariantType = "ARKIV"
        private const val ConsumerIdHeaderKey = "Nav-Consumer-Id"
        private const val ConsumerIdHeaderValue = "k9-punsj"
        private const val CorrelationIdHeader = "Nav-Callid"
        private const val MaxDokumentSize = 5 * 1024 * 1024
    }

    private val client = WebClient
            .builder()
            .baseUrl(safBaseUrl.toString())
            .exchangeStrategies(
                    ExchangeStrategies.builder()
                            .codecs { configurer ->
                                configurer
                                        .defaultCodecs()
                                        .maxInMemorySize(MaxDokumentSize)
                            }.build()
            )
            .build()

    suspend fun opprettOppgave(aktørid: String, joarnalpostId: String) {
        val opprettOppgaveRequest = OpprettOppgaveRequest(aktivDato = LocalDate.now(),
                aktoerId = aktørid,
                journalpostId = joarnalpostId,
                oppgavetype = "JFR",
                prioritet = Prioritet.NORM,
                tema = "OMS")
        val response = client
                .post()
                .uri { it.pathSegment("api", "v1", "oppgaver").build() }
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper().writeValueAsString(opprettOppgaveRequest))
                .retrieve()
                .toEntity(SafDtos.JournalpostResponseWrapper::class.java)
                .awaitFirst()

    }


}