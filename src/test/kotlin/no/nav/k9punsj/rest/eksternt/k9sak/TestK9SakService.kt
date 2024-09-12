package no.nav.k9punsj.rest.eksternt.k9sak

import no.nav.k9.kodeverk.dokument.Brevkode
import no.nav.k9.sak.kontrakt.opplæringspenger.godkjentopplaeringsinstitusjon.GodkjentOpplæringsinstitusjonDto
import no.nav.k9.sak.typer.Periode
import no.nav.k9.sak.typer.Saksnummer
import no.nav.k9.søknad.Søknad
import no.nav.k9punsj.felles.PunsjFagsakYtelseType
import no.nav.k9punsj.felles.dto.ArbeidsgiverMedArbeidsforholdId
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.felles.dto.SaksnummerDto
import no.nav.k9punsj.felles.dto.SøknadEntitet
import no.nav.k9punsj.integrasjoner.k9sak.K9SakService
import no.nav.k9punsj.integrasjoner.k9sak.dto.Fagsak
import no.nav.k9punsj.integrasjoner.k9sak.dto.ReserverSaksnummerDto
import no.nav.k9punsj.integrasjoner.k9sak.dto.ReservertSaksnummerDto
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.*

@Component
@Profile("test") // TODO: Erstatt med mock
internal class TestK9SakService : K9SakService {
    override suspend fun hentPerioderSomFinnesIK9(
        søker: String,
        barn: String?,
        punsjFagsakYtelseType: PunsjFagsakYtelseType
    ): Pair<List<PeriodeDto>?, String?> {
        // OmsorgspengerutbetalingRoutesTest.Korrigering OMP UT med fraværsperioder fra tidiger år validerer riktigt år
        if (søker == "03011939596" && punsjFagsakYtelseType == PunsjFagsakYtelseType.OMSORGSPENGER) {
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
        punsjFagsakYtelseType: PunsjFagsakYtelseType,
        periode: PeriodeDto
    ): Pair<List<PeriodeDto>?, String?> {
        return hentPerioderSomFinnesIK9(søker = søker, barn = barn, punsjFagsakYtelseType = punsjFagsakYtelseType)
    }

    override suspend fun hentArbeidsforholdIdFraInntektsmeldinger(
        søker: String,
        punsjFagsakYtelseType: PunsjFagsakYtelseType,
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
                relatertPersonAktørId = null,
                gyldigPeriode = null
            )
        ),
        second = null
    )

    override suspend fun hentEllerOpprettSaksnummer(
        k9FormatSøknad: Søknad,
        søknadEntitet: SøknadEntitet,
        punsjFagsakYtelseType: PunsjFagsakYtelseType
    ): Pair<String?, String?> {
        return Pair("ABC123", null)
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
                gyldigePerioder
            ),
        )
    }
}
