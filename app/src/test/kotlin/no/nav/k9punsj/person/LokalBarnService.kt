package no.nav.k9punsj.person

import no.nav.k9punsj.LokalProfil
import no.nav.k9punsj.util.MockUtil.erFødtI
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.Month

@Component
@LokalProfil
internal class LokalBarnService : BarnService {
    override suspend fun hentBarn(identitetsnummer: String): Set<Barn> = when {
        identitetsnummer.erFødtI(Month.MAY) -> setOf(
            Barn(
                identitetsnummer = "09018070097", // Tilfeldig genrerert gyldig fnr
                fødselsdato = LocalDate.now().minusYears(10),
                fornavn = "Ola",
                etternavn = "Nordmann"
            ),
            Barn(
                identitetsnummer = "08078374668",
                fødselsdato = LocalDate.now().minusYears(5).minusMonths(5),
                fornavn = "Kari",
                mellomnavn = "Mellomste", // Tilfeldig genrerert gyldig fnr
                etternavn = "Nordmann"
            )
        )
        else -> setOf()
    }
}