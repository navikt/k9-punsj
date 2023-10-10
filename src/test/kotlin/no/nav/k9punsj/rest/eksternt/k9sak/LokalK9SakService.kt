package no.nav.k9punsj.rest.eksternt.k9sak

import no.nav.k9.kodeverk.dokument.Brevkode
import no.nav.k9.søknad.Søknad
import no.nav.k9punsj.LokalProfil
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.felles.dto.ArbeidsgiverMedArbeidsforholdId
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.felles.dto.SøknadEntitet
import no.nav.k9punsj.integrasjoner.k9sak.Fagsak
import no.nav.k9punsj.integrasjoner.k9sak.K9SakService
import no.nav.k9punsj.util.MockUtil.erFødtI
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.Month

@Component
@LokalProfil
class LokalK9SakService : K9SakService {
    override suspend fun hentPerioderSomFinnesIK9(
        søker: String,
        barn: String?,
        fagsakYtelseType: FagsakYtelseType
    ) = when (søker.erFødtI(Month.MAY)) {
        true -> Pair(
            listOf(
                PeriodeDto(fom = LocalDate.now(), tom = LocalDate.now().plusWeeks(3)),
                PeriodeDto(fom = LocalDate.now().minusMonths(6), tom = LocalDate.now().minusMonths(4))
            ),
            null
        )
        false -> Pair(emptyList(), null)
    }

    override suspend fun hentPerioderSomFinnesIK9ForPeriode(
        søker: String,
        barn: String?,
        fagsakYtelseType: FagsakYtelseType,
        periode: PeriodeDto
    ): Pair<List<PeriodeDto>?, String?> {
        return hentPerioderSomFinnesIK9(søker = søker, barn = barn, fagsakYtelseType = fagsakYtelseType)
    }

    override suspend fun hentArbeidsforholdIdFraInntektsmeldinger(
        søker: String,
        fagsakYtelseType: FagsakYtelseType,
        periodeDto: PeriodeDto
    ): Pair<List<ArbeidsgiverMedArbeidsforholdId>?, String?> = Pair(emptyList(), null)

    override suspend fun hentFagsaker(søker: String): Pair<Set<Fagsak>?, String?> = Pair(
        first = setOf(
            Fagsak(
                saksnummer = "ABC123",
                no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
                null,
                gyldigPeriode = PeriodeDto(LocalDate.parse("2022-08-01"), LocalDate.parse("2022-08-15"))
            ),
            Fagsak(
                saksnummer = "DEF456",
                no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE,
                null,
                gyldigPeriode = PeriodeDto(LocalDate.parse("2022-08-01"), LocalDate.parse("2022-08-15"))
            ),
            Fagsak(
                saksnummer = "GHI789",
                no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_KS,
                null,
                gyldigPeriode = null
            )
        ),
        second = null
    )

    override suspend fun hentEllerOpprettSaksnummer(
        søknadEntitet: SøknadEntitet,
        fagsakYtelseType: no.nav.k9punsj.felles.FagsakYtelseType
    ): Pair<String?, String?> {
        return Pair("ABC123", null)
    }

    override suspend fun sendInnSoeknad(
        soknad: Søknad,
        journalpostId: String,
        fagsakYtelseType: FagsakYtelseType,
        saksnummer: String,
        brevkode: Brevkode
    ) {
        // do nothing
    }
}
