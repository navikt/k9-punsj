package no.nav.k9punsj.rest.eksternt.punsjbollen

import no.nav.k9punsj.rest.web.dto.NorskIdentDto
import no.nav.k9punsj.rest.web.dto.SaksnummerDto
import java.time.LocalDate

interface PunsjbolleService {

    suspend fun opprettEllerHentFagsaksnummer(søker: NorskIdentDto, barn: NorskIdentDto, fraOgMed: LocalDate): SaksnummerDto?
}
