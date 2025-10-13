package no.nav.k9punsj.rest.eksternt.k9sak

import no.nav.k9.kodeverk.dokument.Brevkode
import no.nav.k9.sak.kontrakt.opplæringspenger.godkjentopplaeringsinstitusjon.GodkjentOpplæringsinstitusjonDto
import no.nav.k9.sak.typer.Periode
import no.nav.k9.sak.typer.Saksnummer
import no.nav.k9.søknad.Søknad
import no.nav.k9punsj.LokalProfil
import no.nav.k9punsj.felles.PunsjFagsakYtelseType
import no.nav.k9punsj.felles.dto.ArbeidsgiverMedArbeidsforholdId
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.felles.dto.SaksnummerDto
import no.nav.k9punsj.felles.dto.SøknadEntitet
import no.nav.k9punsj.integrasjoner.k9sak.K9SakService
import no.nav.k9punsj.integrasjoner.k9sak.dto.Fagsak
import no.nav.k9punsj.integrasjoner.k9sak.dto.ReserverSaksnummerDto
import no.nav.k9punsj.integrasjoner.k9sak.dto.HentK9SaksnummerGrunnlag
import no.nav.k9punsj.integrasjoner.k9sak.dto.ReservertSaksnummerDto
import no.nav.k9punsj.util.MockUtil.erFødtI
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.Month
import java.util.*

@Component
@LokalProfil
class LokalK9SakService : K9SakService {
    override suspend fun hentPerioderSomFinnesIK9(
        søker: String,
        barn: String?,
        punsjFagsakYtelseType: PunsjFagsakYtelseType
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

    override suspend fun hentPerioderSomFinnesIK9ForSaksnummer(saksnummer: String): Pair<List<PeriodeDto>?, String?> {
        return Pair(emptyList(), null)
    }

    override suspend fun hentPerioderSomFinnesIK9ForPeriode(
        søker: String,
        barn: String?,
        punsjFagsakYtelseType: PunsjFagsakYtelseType,
        periode: PeriodeDto
    ): Pair<List<PeriodeDto>?, String?> {
        return hentPerioderSomFinnesIK9(søker = søker, barn = barn, punsjFagsakYtelseType = punsjFagsakYtelseType)
    }

    override suspend fun hentArbeidsforholdIdFraInntektsmeldinger(
        søker: String,
        punsjFagsakYtelseType: PunsjFagsakYtelseType,
        periodeDto: PeriodeDto
    ): Pair<List<ArbeidsgiverMedArbeidsforholdId>?, String?> = Pair(emptyList(), null)

    override suspend fun hentFagsaker(søker: String): Pair<Set<Fagsak>?, String?> = Pair(
        first = setOf(
            Fagsak(
                saksnummer = "ABC123",
                no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
                null,
                gyldigPeriode = PeriodeDto(LocalDate.parse("2022-08-01"), LocalDate.parse("2022-08-15")),
                relatertPersonAktørId = null
            ),
            Fagsak(
                saksnummer = "DEF456",
                no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE,
                null,
                gyldigPeriode = PeriodeDto(LocalDate.parse("2022-08-01"), LocalDate.parse("2022-08-15")),
                relatertPersonAktørId = null
            ),
            Fagsak(
                saksnummer = "GHI789",
                no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_KS,
                null,
                gyldigPeriode = null,
                relatertPersonAktørId = null
            ),
            Fagsak(
                saksnummer = "JKL123",
                no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER,
                null,
                gyldigPeriode = PeriodeDto(LocalDate.parse("2022-08-01"), LocalDate.parse("2022-08-15")),
                relatertPersonAktørId = null
            )
        ),
        second = null
    )

    override suspend fun hentEllerOpprettSaksnummer(
        k9FormatSøknad: Søknad,
        søknadEntitet: SøknadEntitet,
        punsjFagsakYtelseType: no.nav.k9punsj.felles.PunsjFagsakYtelseType
    ): Pair<String?, String?> {
        return Pair("ABC123", null)
    }

    override suspend fun hentEllerOpprettSaksnummer(hentK9SaksnummerGrunnlag: HentK9SaksnummerGrunnlag): String {
        return "ABC123"
    }

    override suspend fun sendInnSoeknad(
        soknad: Søknad,
        journalpostId: String,
        punsjFagsakYtelseType: PunsjFagsakYtelseType,
        saksnummer: String,
        brevkode: Brevkode
    ) {
        // do nothing
    }

    override suspend fun reserverSaksnummer(reserverSaksnummerDto: ReserverSaksnummerDto) = SaksnummerDto("ABC123")
    override suspend fun hentReservertSaksnummer(saksnummer: Saksnummer) = ReservertSaksnummerDto(
        saksnummer = "ABC123",
        ytelseType = no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
        brukerAktørId = "123456789",
    )

    override suspend fun hentReserverteSaksnummere(søkerAktørId: String): Set<ReservertSaksnummerDto> {
        return setOf(
            ReservertSaksnummerDto(
                saksnummer = "ABC123",
                ytelseType = no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
                brukerAktørId = "123456789",
            ),
            ReservertSaksnummerDto(
                saksnummer = "DEF456",
                ytelseType = no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE,
                brukerAktørId = "123456789",
            ),
            ReservertSaksnummerDto(
                saksnummer = "GHI789",
                ytelseType = no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_KS,
                brukerAktørId = "123456789",
            ),
            ReservertSaksnummerDto(
                saksnummer = "JKL123",
                ytelseType = no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER,
                brukerAktørId = "123456789",
            )
        )
    }

    override suspend fun opprettSakOgSendInnSøknad(
        soknad: Søknad,
        søknadEntitet: SøknadEntitet,
        journalpostId: String,
        punsjFagsakYtelseType: PunsjFagsakYtelseType,
        saksnummer: String,
        brevkode: Brevkode,
    ) {
        // do nothing
    }

    override suspend fun hentInstitusjoner(): List<GodkjentOpplæringsinstitusjonDto> {
        val gyldigePerioder = listOf(Periode(LocalDate.now().minusMonths(12), LocalDate.now()))
        return listOf(
            GodkjentOpplæringsinstitusjonDto(
                UUID.fromString("dccf90ba-5b3f-475b-9026-af0fa771d6c1"),
                "Sykehus Asker/Bærum",
                "000000000",
                gyldigePerioder
            ),
            GodkjentOpplæringsinstitusjonDto(
                UUID.fromString("ce14ae31-b365-48e6-becb-efa5450dbd7c"),
                "Sykehuset Buskerud (Drammen sykehus)",
                "000000001",
                gyldigePerioder
            ),
            GodkjentOpplæringsinstitusjonDto(
                UUID.fromString("7b2f8fac-8863-4794-b97d-aa35bc8e43f1"),
                "Sykehuset i Vestfold",
                "000000002",
                gyldigePerioder
            ),
            GodkjentOpplæringsinstitusjonDto(
                UUID.fromString("202c3f51-fc70-4ecb-8bf1-2eb20daf6346"),
                "Sykehus Asker/Bærum",
                "000000003",
                gyldigePerioder
            ),
            GodkjentOpplæringsinstitusjonDto(
                UUID.fromString("7a53ef39-4cac-4866-a8ae-3784af75fc91"),
                "Sykehuset Innlandet Elverum",
                "000000004",
                gyldigePerioder
            ),
            GodkjentOpplæringsinstitusjonDto(
                UUID.fromString("731adb3d-cfed-4ae1-a972-2f270236cc78"),
                "Sykehuset Innlandet Gjøvik",
                "000000005",
                gyldigePerioder
            ),
            GodkjentOpplæringsinstitusjonDto(
                UUID.fromString("df816f00-7407-45e9-a6fb-abb4c4cd5b9a"),
                "Sykehuset Innlandet Hamar",
                "000000006",
                gyldigePerioder
            ),
            GodkjentOpplæringsinstitusjonDto(
                UUID.fromString("455d2fb6-68e6-48c0-9100-9a30b17228a5"),
                "Sykehuset Innlandet Kongsvinger",
                "000000007",
                gyldigePerioder
            ),
            GodkjentOpplæringsinstitusjonDto(
                UUID.fromString("2562d2f5-4038-47c7-a007-e720b83abf50"),
                "Sykehuset Innlandet Lillehammer",
                "000000008",
                gyldigePerioder
            ),
            GodkjentOpplæringsinstitusjonDto(
                UUID.fromString("1cbbc81e-4193-4dfa-a622-d01cbb577203"),
                "Sykehuset Innlandet Tynset",
                "000000009",
                gyldigePerioder
            ),
            GodkjentOpplæringsinstitusjonDto(
                UUID.fromString("87ca816a-0361-42d5-ad34-a0cff1446fb0"),
                "Sykehuset Telemark",
                "000000010",
                gyldigePerioder
            ),
            GodkjentOpplæringsinstitusjonDto(
                UUID.fromString("1661b54c-10f8-4d01-9e94-487551d9f0f9"),
                "Sykehuset Østfold",
                "000000011",
                gyldigePerioder
            ),
            GodkjentOpplæringsinstitusjonDto(
                UUID.fromString("3a4d57d6-4022-4e8c-9236-d3f6f366e13b"),
                "Sørlandet sykehus, Arendal",
                "000000012",
                gyldigePerioder
            ),
            GodkjentOpplæringsinstitusjonDto(
                UUID.fromString("8f29f0ea-3d59-4a39-9e2e-e81b7f562eaf"),
                "Sørlandet sykehus, Farsund",
                "000000013",
                gyldigePerioder
            ),
            GodkjentOpplæringsinstitusjonDto(
                UUID.fromString("617f81fb-6405-456c-a270-6620dcbf624c"),
                "Sørlandet sykehus, Kristiansand",
                "000000014",
                gyldigePerioder
            ),
            GodkjentOpplæringsinstitusjonDto(
                UUID.fromString("fe402564-c764-4acc-b23d-6e6a05389ff2"),
                "Sørlandet sykehus, Mandal",
                "000000015",
                gyldigePerioder
            ),
            GodkjentOpplæringsinstitusjonDto(
                UUID.fromString("1879b639-3505-4437-a936-0d4b7521f314"),
                "St. Olavs Hospital",
                "000000016",
                gyldigePerioder
            ),
            GodkjentOpplæringsinstitusjonDto(
                UUID.fromString("66c45d21-885b-4d8d-9a44-7d26fd8450b3"),
                "Stavanger Universitetssykehus",
                "000000017",
                gyldigePerioder
            ),
            GodkjentOpplæringsinstitusjonDto(
                UUID.fromString("b85d7f35-4b71-49a0-9522-7c30d85ea747"),
                "Sunnaas sykehus",
                "000000018",
                gyldigePerioder
            ),
        )
    }
}
