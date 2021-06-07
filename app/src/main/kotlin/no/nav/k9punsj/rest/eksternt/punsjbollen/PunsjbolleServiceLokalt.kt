package no.nav.k9punsj.rest.eksternt.punsjbollen

import no.nav.k9punsj.rest.web.dto.JournalpostIdDto
import no.nav.k9punsj.rest.web.dto.NorskIdentDto
import no.nav.k9punsj.rest.web.dto.SaksnummerDto
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("local")
class PunsjbolleServiceLokalt : PunsjbolleService {
    override suspend fun opprettEllerHentFagsaksnummer(
        søker: NorskIdentDto,
        barn: NorskIdentDto,
        journalpostIdDto: JournalpostIdDto
    ): SaksnummerDto? {
        return SaksnummerDto("")
    }
}
