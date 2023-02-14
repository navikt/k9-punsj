package no.nav.k9punsj.integrasjoner.punsjbollen

import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9punsj.felles.dto.PeriodeDto
import java.util.UUID

interface PunsjbolleService {
    suspend fun opprettEllerHentFagsaksnummer(
        søker: String,
        pleietrengende: String? = null,
        annenPart: String? = null,
        journalpostId: String?,
        periode: PeriodeDto?,
        fagsakYtelseType: FagsakYtelseType,
        correlationId: String = UUID.randomUUID().toString()
    ): SaksnummerDto
}
