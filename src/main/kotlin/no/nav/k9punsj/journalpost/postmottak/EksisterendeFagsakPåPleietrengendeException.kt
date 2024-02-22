package no.nav.k9punsj.journalpost.postmottak

import no.nav.k9punsj.integrasjoner.k9sak.dto.Fagsak
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.ErrorResponseException
import java.net.URI

private val CONFLICT_STATUS = HttpStatus.CONFLICT

class EksisterendeFagsakPåPleietrengendeException(
    journalpostId: String,
    eksisterendeFagsak: Fagsak,
) : ErrorResponseException(CONFLICT_STATUS, asProblemDetail(journalpostId, eksisterendeFagsak), null) {
    private companion object {
        private fun asProblemDetail(
            journalpostId: String,
            eksisterendeFagsak: Fagsak,
        ): ProblemDetail {
            val conflict = HttpStatus.CONFLICT
            val problemDetail = ProblemDetail.forStatus(conflict)
            problemDetail.title = "Eksisterende fagsak på pleietrengende"

            problemDetail.detail =
                "Det eksisterer allerede en fagsak(${eksisterendeFagsak.sakstype.name} - ${eksisterendeFagsak.saksnummer}) på pleietrengende. JournalpostId = $journalpostId."

            problemDetail.type = URI("/problem-details/eksisterende-fagsak-på-pleietrengende")

            return problemDetail
        }
    }
}
