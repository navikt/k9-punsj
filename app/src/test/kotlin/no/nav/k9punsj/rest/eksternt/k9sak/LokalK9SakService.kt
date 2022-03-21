package no.nav.k9punsj.rest.eksternt.k9sak

import no.nav.k9punsj.LokalProfil
import no.nav.k9punsj.db.datamodell.FagsakYtelseType
import no.nav.k9punsj.db.datamodell.NorskIdent
import no.nav.k9punsj.integrasjoner.k9sak.Fagsak
import no.nav.k9punsj.integrasjoner.k9sak.K9SakService
import no.nav.k9punsj.domenetjenester.dto.ArbeidsgiverMedArbeidsforholdId
import no.nav.k9punsj.domenetjenester.dto.PeriodeDto
import no.nav.k9punsj.util.MockUtil.erFødtI
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.Month

@Component
@LokalProfil
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

    override suspend fun hentArbeidsforholdIdFraInntektsmeldinger(
        søker: NorskIdent,
        fagsakYtelseType: FagsakYtelseType,
        periodeDto: PeriodeDto,
    ) : Pair<List<ArbeidsgiverMedArbeidsforholdId>?, String?> = Pair(emptyList(), null)

    override suspend fun hentFagsaker(søker: String): Pair<Set<Fagsak>?, String?> = Pair(
        first = setOf(
            Fagsak(saksnummer = "ABC123", no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN),
            Fagsak(saksnummer = "DEF456", no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE),
            Fagsak(saksnummer = "GHI789", no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_KS)
        ),
        second = null
    )
}
