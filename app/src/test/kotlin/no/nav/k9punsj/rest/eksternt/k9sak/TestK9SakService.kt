package no.nav.k9punsj.rest.eksternt.k9sak

import no.nav.k9punsj.TestProfil
import no.nav.k9punsj.felles.AktørId
import no.nav.k9punsj.felles.CorrelationId.Companion.somCorrelationId
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.felles.K9Saksnummer.Companion.somK9Saksnummer
import no.nav.k9punsj.felles.dto.ArbeidsgiverMedArbeidsforholdId
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.integrasjoner.infotrygd.PunsjbolleSøknadstype
import no.nav.k9punsj.integrasjoner.k9sak.Fagsak
import no.nav.k9punsj.integrasjoner.k9sak.HentK9SaksnummerGrunnlag
import no.nav.k9punsj.integrasjoner.k9sak.K9SakService
import no.nav.k9punsj.integrasjoner.k9sak.LopendeSakDto
import no.nav.k9punsj.ruting.RutingGrunnlag
import org.springframework.stereotype.Component
import java.time.LocalDate

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
        k9SaksnummerGrunnlag: HentK9SaksnummerGrunnlag,
        opprettNytt: Boolean
    ): Pair<String?, String?> {
        return when(opprettNytt) {
            true -> Pair("NEW123", null)
            false -> Pair("OLD123", null)
        }
    }

    override suspend fun harLopendeSakSomInvolvererEnAv(lopendeSakDto: LopendeSakDto): RutingGrunnlag {
        TODO("Not yet implemented")
    }

    override suspend fun inngårIUnntaksliste(aktørIder: Set<AktørId>): Boolean {
        TODO("Not yet implemented")
    }
}
