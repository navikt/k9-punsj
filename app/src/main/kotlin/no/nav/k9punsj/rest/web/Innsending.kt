package no.nav.k9punsj.rest.web

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.k9punsj.db.datamodell.NorskIdent
import no.nav.k9punsj.rest.web.dto.JournalpostIdDto
import no.nav.k9punsj.rest.web.dto.NorskIdentDto
import no.nav.k9punsj.rest.web.dto.SøknadIdDto
import org.springframework.http.HttpStatus

internal val objectMapper = jacksonObjectMapper()

typealias JournalpostId = String

typealias SøknadJson = MutableMap<String, Any?>

data class OpprettNySøknad(
    val norskIdent: NorskIdentDto,
    val journalpostId: JournalpostIdDto,
)

data class HentSøknad(
    val norskIdent: NorskIdent,
)

data class SendSøknad(
    val norskIdent: NorskIdentDto,
    val soeknadId: SøknadIdDto,
)

data class Matchfagsak(
    val brukerIdent: NorskIdentDto,
    val barnIdent: NorskIdentDto,
)

data class PunsjBolleDto(
    val brukerIdent: NorskIdentDto,
    val barnIdent: NorskIdentDto,
    val journalpostId: JournalpostIdDto,
)


internal fun Boolean.httpStatus() = if (this) HttpStatus.OK else HttpStatus.BAD_REQUEST

