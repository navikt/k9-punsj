package no.nav.k9punsj.journalpost.postmottak

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.ErrorResponseException
import java.net.URI

class PostMottakException(
    melding: String,
    httpStatus: HttpStatus,
    journalpostId: String,
) : ErrorResponseException(httpStatus, asProblemDetail(melding, httpStatus, journalpostId), null) {
    private companion object {
        private fun asProblemDetail(
            melding: String,
            httpStatus: HttpStatus,
            journalpostId: String,
        ): ProblemDetail {
            val problemDetail = ProblemDetail.forStatus(httpStatus)
            problemDetail.title = "Feil ved journalføring av journalpost $journalpostId"
            problemDetail.detail = melding

            problemDetail.type = URI("/problem-details/post-mottak")

            return problemDetail
        }
    }
}
