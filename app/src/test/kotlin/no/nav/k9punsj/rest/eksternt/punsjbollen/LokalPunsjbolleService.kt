package no.nav.k9punsj.rest.eksternt.punsjbollen

import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9.søknad.Søknad
import no.nav.k9punsj.CorrelationId
import no.nav.k9punsj.LokalProfil
import no.nav.k9punsj.integrasjoner.punsjbollen.PunsjbolleRuting
import no.nav.k9punsj.integrasjoner.punsjbollen.PunsjbolleService
import no.nav.k9punsj.domenetjenester.dto.JournalpostIdDto
import no.nav.k9punsj.domenetjenester.dto.NorskIdentDto
import no.nav.k9punsj.domenetjenester.dto.PeriodeDto
import no.nav.k9punsj.domenetjenester.dto.SaksnummerDto
import org.springframework.stereotype.Component

@Component
@LokalProfil
internal class LokalPunsjbolleService : PunsjbolleService {
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
    }.let { SaksnummerDto("SAK1") }

    override suspend fun opprettEllerHentFagsaksnummer(
        søker: NorskIdentDto,
        pleietrengende: NorskIdentDto?,
        annenPart: NorskIdentDto?,
        søknad: Søknad,
        correlationId: CorrelationId
    ) = SaksnummerDto("SAK1")

    override suspend fun ruting(
        søker: NorskIdentDto,
        pleietrengende: NorskIdentDto?,
        annenPart: NorskIdentDto?,
        journalpostId: JournalpostIdDto?,
        periode: PeriodeDto?,
        correlationId: CorrelationId,
        fagsakYtelseType: FagsakYtelseType,
    ) = when (journalpostId) {
        "45537868838" -> PunsjbolleRuting.IkkeStøttet
        "463687943" -> PunsjbolleRuting.Infotrygd
        else -> PunsjbolleRuting.K9Sak
    }
}
