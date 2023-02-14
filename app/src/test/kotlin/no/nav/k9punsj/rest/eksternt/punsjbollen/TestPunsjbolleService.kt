package no.nav.k9punsj.rest.eksternt.punsjbollen

import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9punsj.TestProfil
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.integrasjoner.punsjbollen.PunsjbolleService
import no.nav.k9punsj.integrasjoner.punsjbollen.SaksnummerDto
import org.springframework.stereotype.Component

@Component
@TestProfil
internal class TestPunsjbolleService : PunsjbolleService {
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
    }.let { SaksnummerDto("133742069666") }

}
