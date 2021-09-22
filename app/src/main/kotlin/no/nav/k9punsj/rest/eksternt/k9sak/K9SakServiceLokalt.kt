package no.nav.k9punsj.rest.eksternt.k9sak

import no.nav.k9punsj.db.datamodell.FagsakYtelseType
import no.nav.k9punsj.db.datamodell.NorskIdent
import no.nav.k9punsj.rest.web.dto.PeriodeDto
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.time.LocalDate

@Configuration
@Profile("local")
class K9SakServiceLokalt : K9SakService {
    override suspend fun hentPerioderSomFinnesIK9(
        søker: NorskIdent,
        barn: NorskIdent,
        fagsakYtelseType: FagsakYtelseType,
    ) = when (søker.erFødtIMai()) {
        true -> Pair(listOf(
            PeriodeDto(fom = LocalDate.now(), tom = LocalDate.now().plusWeeks(3)),
            PeriodeDto(fom = LocalDate.now().minusMonths(6), tom = LocalDate.now().minusMonths(4))
        ), null)
        false -> Pair(emptyList(), null)
    }

    private companion object{
        fun String.erFødtIMai() = kotlin.runCatching {
            substring(2,4) == "05"
        }.fold(onSuccess = { it }, onFailure = {false} )
    }
}