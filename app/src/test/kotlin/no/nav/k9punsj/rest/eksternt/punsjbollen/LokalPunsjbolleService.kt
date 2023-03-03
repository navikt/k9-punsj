package no.nav.k9punsj.rest.eksternt.punsjbollen

import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9punsj.LokalProfil
import no.nav.k9punsj.felles.PunsjbolleRuting
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.integrasjoner.punsjbollen.PunsjbolleService
import no.nav.k9punsj.integrasjoner.punsjbollen.SaksnummerDto
import org.springframework.stereotype.Component

@Component
@LokalProfil
internal class LokalPunsjbolleService : PunsjbolleService {
    override suspend fun opprettEllerHentFagsaksnummer(
        søker: String,
        pleietrengende: String?,
        annenPart: String?,
        journalpostId: String?,
        periode: PeriodeDto?,
        fagsakYtelseType: FagsakYtelseType,
        correlationId: String
    ) = require(journalpostId != null || periode != null) {
        "Må sette minst en av journalpostId og periode"
    }.let { SaksnummerDto("SAK1") }

    override suspend fun ruting(
        søker: String,
        pleietrengende: String?,
        annenPart: String?,
        journalpostId: String?,
        periode: PeriodeDto?,
        fagsakYtelseType: FagsakYtelseType,
        correlationId: String
    ) = when (journalpostId) {
        "45537868838" -> PunsjbolleRuting.IkkeStøttet
        "463687943" -> PunsjbolleRuting.Infotrygd
        else -> PunsjbolleRuting.K9Sak
    }
}
