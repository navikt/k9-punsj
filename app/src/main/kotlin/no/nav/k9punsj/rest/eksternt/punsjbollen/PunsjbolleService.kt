package no.nav.k9punsj.rest.eksternt.punsjbollen

import no.nav.k9punsj.CorrelationId
import no.nav.k9punsj.rest.web.dto.JournalpostIdDto
import no.nav.k9punsj.rest.web.dto.NorskIdentDto
import no.nav.k9punsj.rest.web.dto.PeriodeDto
import no.nav.k9punsj.rest.web.dto.SaksnummerDto

interface PunsjbolleService {
    suspend fun opprettEllerHentFagsaksnummer(
        søker: NorskIdentDto,
        barn: NorskIdentDto,
        journalpostId: JournalpostIdDto?,
        periode: PeriodeDto?,
        correlationId: CorrelationId): SaksnummerDto?
}
