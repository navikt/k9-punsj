package no.nav.k9punsj.rest.eksternt.k9sak

import no.nav.k9punsj.TestProfil
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.felles.dto.ArbeidsgiverMedArbeidsforholdId
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.integrasjoner.k9sak.Fagsak
import no.nav.k9punsj.integrasjoner.k9sak.K9SakService
import org.springframework.stereotype.Component

@Component
@TestProfil
internal class TestK9SakService : K9SakService {
    override suspend fun hentPerioderSomFinnesIK9(
        søker: String,
        barn: String,
        fagsakYtelseType: FagsakYtelseType
    ): Pair<List<PeriodeDto>?, String?> = Pair(emptyList(), null)

    override suspend fun hentArbeidsforholdIdFraInntektsmeldinger(
        søker: String,
        fagsakYtelseType: FagsakYtelseType,
        periodeDto: PeriodeDto
    ): Pair<List<ArbeidsgiverMedArbeidsforholdId>?, String?> {
        // simulerer svar på denne
        return if (søker == "02020050123") {
            Pair(listOf(ArbeidsgiverMedArbeidsforholdId("randomOrgNummer", listOf("randomArbeidsforholdId"))), null)
        } else {
            Pair(emptyList(), null)
        }
    }

    override suspend fun hentFagsaker(søker: String): Pair<Set<Fagsak>?, String?> = Pair(
        first = setOf(
            Fagsak(saksnummer = "ABC123", no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN),
            Fagsak(saksnummer = "DEF456", no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE),
            Fagsak(saksnummer = "GHI789", no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_KS)
        ),
        second = null
    )
}
