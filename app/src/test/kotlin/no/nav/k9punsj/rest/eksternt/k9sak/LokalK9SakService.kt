package no.nav.k9punsj.rest.eksternt.k9sak

import no.nav.k9punsj.db.datamodell.FagsakYtelseType
import no.nav.k9punsj.db.datamodell.NorskIdent
import no.nav.k9punsj.rest.web.dto.PeriodeDto
import no.nav.k9punsj.util.MockUtil.erFødtI
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.Month

@Component
@Profile("local")
class LokalK9SakService : K9SakService {
    override suspend fun hentPerioderSomFinnesIK9(
        søker: NorskIdent,
        barn: NorskIdent,
        fagsakYtelseType: FagsakYtelseType,
    ) = when (søker.erFødtI(Month.MAY)) {
        true -> Pair(listOf(
            PeriodeDto(fom = LocalDate.now(), tom = LocalDate.now().plusWeeks(3)),
            PeriodeDto(fom = LocalDate.now().minusMonths(6), tom = LocalDate.now().minusMonths(4))
        ), null)
        false -> Pair(emptyList(), null)
    }
}