package no.nav.k9punsj.rest.eksternt.k9sak

import no.nav.k9punsj.TestProfil
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.felles.dto.ArbeidsgiverMedArbeidsforholdId
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.felles.dto.SaksnummerDto
import no.nav.k9punsj.integrasjoner.k9sak.Fagsak
import no.nav.k9punsj.integrasjoner.k9sak.HentK9SaksnummerGrunnlag
import no.nav.k9punsj.integrasjoner.k9sak.K9SakService
import no.nav.k9punsj.integrasjoner.k9sak.dto.PunsjetSøknad
import no.nav.k9punsj.integrasjoner.k9sak.dto.SendPunsjetSoeknadTilK9SakGrunnlag
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
@TestProfil
internal class TestK9SakService : K9SakService {
    override suspend fun hentPerioderSomFinnesIK9(
        søker: String,
        barn: String?,
        fagsakYtelseType: FagsakYtelseType
    ): Pair<List<PeriodeDto>?, String?> {
        // OmsorgspengerutbetalingRoutesTest.Korrigering OMP UT med fraværsperioder fra tidiger år validerer riktigt år
        if (søker == "03011939596" && fagsakYtelseType == FagsakYtelseType.OMSORGSPENGER) {
            return Pair(
                listOf(
                    PeriodeDto(
                        fom = LocalDate.of(2022, 12, 1),
                        tom = LocalDate.of(2022, 12, 5)
                    ),
                    PeriodeDto(
                        fom = LocalDate.of(2022, 12, 10),
                        tom = LocalDate.of(2022, 12, 15)
                    )
                ),
                null
            )
        }
        return Pair(emptyList(), null)
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
        k9SaksnummerGrunnlag: HentK9SaksnummerGrunnlag
    ): Pair<String?, String?> {
        return Pair("ABC123", null)
    }

    override suspend fun hentSisteSaksnummerForPeriode(
        fagsakYtelseType: FagsakYtelseType,
        periode: PeriodeDto?,
        søker: String,
        pleietrengende: String?
    ): Pair<SaksnummerDto?, String?> {
        TODO("Not yet implemented")
    }

    override suspend fun sendInnSoeknad(soeknad: PunsjetSøknad, grunnlag: SendPunsjetSoeknadTilK9SakGrunnlag) {
        TODO("Not yet implemented")
    }
}
