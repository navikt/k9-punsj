package no.nav.k9punsj.felles

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.ErrorResponseException
import java.net.URI

internal class IkkeStøttetJournalpost(feil: String = "Punsj støtter ikke denne journalposten.") : Throwable(feil)
internal class NotatUnderArbeidFeil : Throwable("Notatet må ferdigstilles før det kan åpnes i Punsj")
internal class IkkeTilgang(feil: String) : Throwable(feil)
internal class FeilIAksjonslogg(feil: String) : Throwable(feil)
internal class UgyldigToken(feil: String) : Throwable(feil)
internal class IkkeFunnet : Throwable()
internal class UventetFeil(feil: String) : Throwable(feil)


class RestKallException(
    titel: String,
    message: String,
    httpStatus: HttpStatus,
    uri: URI
) : ErrorResponseException(httpStatus, asProblemDetail(titel, message, httpStatus, uri), null) {
    private companion object {
        private fun asProblemDetail(
            tittel: String,
            message: String,
            httpStatus: HttpStatus,
            uri: URI
        ): ProblemDetail {
            val problemDetail = ProblemDetail.forStatus(httpStatus)
            problemDetail.title = tittel
            problemDetail.detail = message
            problemDetail.type = URI("/problem-details/restkall-feil")
            problemDetail.setProperty("endepunkt", uri)
            return problemDetail
        }
    }
}
