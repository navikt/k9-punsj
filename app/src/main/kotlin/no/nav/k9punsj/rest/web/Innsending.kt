package no.nav.k9punsj.rest.web

import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9punsj.db.datamodell.NorskIdent
import no.nav.k9punsj.domenetjenester.dto.*
import org.springframework.http.HttpStatus

typealias JournalpostId = String

typealias SøknadJson = MutableMap<String, Any?>

data class OpprettNySøknad(
    val norskIdent: NorskIdentDto,
    val journalpostId: JournalpostIdDto,
    val pleietrengendeIdent: NorskIdentDto?,
    val annenPart: NorskIdentDto?,
    //TODO endre til å bare bruke pleietrengendeIdent, men støtter både barnIdent og pleietrengendeIdent
    val barnIdent: NorskIdentDto?,
)

data class HentSøknad(
    val norskIdent: NorskIdent,
)

data class IdentOgJournalpost(
    val norskIdent: NorskIdentDto,
    val journalpostId: JournalpostIdDto,
)

data class SendSøknad(
    val norskIdent: NorskIdentDto,
    val soeknadId: SøknadIdDto,
)

data class Matchfagsak(
    val brukerIdent: NorskIdentDto,
    val barnIdent: NorskIdentDto,
)

data class MatchFagsakMedPeriode(
    val brukerIdent: NorskIdentDto,
    val periodeDto: PeriodeDto
)

data class SøkUferdigJournalposter(
    val aktorIdentDto: AktørIdDto,
    val aktorIdentBarnDto: AktørIdDto?
)

data class PunsjBolleDto(
    val brukerIdent: NorskIdentDto,
    //todo bytt navn til pleietrengende
    val barnIdent: NorskIdentDto?,
    val annenPart: NorskIdentDto?,
    val journalpostId: JournalpostIdDto,
    val fagsakYtelseType: FagsakYtelseType
)

data class SettPåVentDto(
    val soeknadId: SøknadIdDto?
)


data class OpprettNyOmsSøknad(
    val norskIdent: NorskIdentDto,
    val journalpostId: JournalpostIdDto,
)

internal fun Boolean.httpStatus() = if (this) HttpStatus.OK else HttpStatus.BAD_REQUEST
