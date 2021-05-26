package no.nav.k9punsj.rest.eksternt.k9sak

import no.nav.k9punsj.db.datamodell.FagsakYtelseType
import no.nav.k9punsj.db.datamodell.NorskIdent
import no.nav.k9punsj.rest.web.dto.PeriodeDto
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("local")
class K9SakServiceLokalt : K9SakService {
    override suspend fun hentPerioderSomFinnesIK9(
        søker: NorskIdent,
        barn: NorskIdent,
        fagsakYtelseType: FagsakYtelseType,
    ): Pair<List<PeriodeDto>?, String?> {
        return Pair(emptyList(), null)
    }
}
