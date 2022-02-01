package no.nav.k9punsj.rest.eksternt.punsjbollen

import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9.søknad.Søknad
import no.nav.k9punsj.CorrelationId
import no.nav.k9punsj.TestProfil
import no.nav.k9punsj.rest.web.dto.JournalpostIdDto
import no.nav.k9punsj.rest.web.dto.NorskIdentDto
import no.nav.k9punsj.rest.web.dto.PeriodeDto
import no.nav.k9punsj.rest.web.dto.SaksnummerDto
import org.springframework.stereotype.Component

@Component
@TestProfil
internal class TestPunsjbolleService : PunsjbolleService {
    override suspend fun opprettEllerHentFagsaksnummer(
        søker: NorskIdentDto,
        pleietrengende: NorskIdentDto?,
        annenPart: NorskIdentDto?,
        journalpostId: JournalpostIdDto?,
        periode: PeriodeDto?,
        correlationId: CorrelationId,
        fagsakYtelseType: FagsakYtelseType,
    ) = require(journalpostId != null || periode != null) {
        "Må sette minst en av journalpostId og periode"
    }.let { SaksnummerDto("133742069666") }

    override suspend fun opprettEllerHentFagsaksnummer(
        søker: NorskIdentDto,
        pleietrengende: NorskIdentDto?,
        annenPart: NorskIdentDto?,
        søknad: Søknad,
        correlationId: CorrelationId
    ) = SaksnummerDto("133742069666")

    override suspend fun ruting(
        søker: NorskIdentDto,
        pleietrengende: NorskIdentDto?,
        annenPart: NorskIdentDto?,
        journalpostId: JournalpostIdDto?,
        periode: PeriodeDto?,
        correlationId: CorrelationId,
        fagsakYtelseType: FagsakYtelseType,
    ) = PunsjbolleRuting.K9Sak
}
