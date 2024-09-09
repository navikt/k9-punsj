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

    override suspend fun hentInstitusjoner(): Pair<List<GodkjentOpplæringsinstitusjonDto>?, String?> {
        val gyldigePerioder = listOf(Periode(LocalDate.now().minusMonths(12), LocalDate.now()))
        return Pair(
            first = listOf(
                GodkjentOpplæringsinstitusjonDto(UUID.randomUUID(), "Sykehus Asker/Bærum", gyldigePerioder),
                GodkjentOpplæringsinstitusjonDto(UUID.randomUUID(), "Sykehuset Buskerud (Drammen sykehus)", gyldigePerioder),
                GodkjentOpplæringsinstitusjonDto(UUID.randomUUID(), "Sykehuset i Vestfold", gyldigePerioder),
                GodkjentOpplæringsinstitusjonDto(UUID.randomUUID(), "Sykehus Asker/Bærum", gyldigePerioder),
                GodkjentOpplæringsinstitusjonDto(UUID.randomUUID(), "Sykehuset Innlandet Elverum", gyldigePerioder),
                GodkjentOpplæringsinstitusjonDto(UUID.randomUUID(), "Sykehuset Innlandet Gjøvik", gyldigePerioder),
                GodkjentOpplæringsinstitusjonDto(UUID.randomUUID(), "Sykehuset Innlandet Hamar", gyldigePerioder),
                GodkjentOpplæringsinstitusjonDto(UUID.randomUUID(), "Sykehuset Innlandet Kongsvinger", gyldigePerioder),
                GodkjentOpplæringsinstitusjonDto(UUID.randomUUID(), "Sykehuset Innlandet Lillehammer", gyldigePerioder),
                GodkjentOpplæringsinstitusjonDto(UUID.randomUUID(), "Sykehuset Innlandet Tynset", gyldigePerioder),
                GodkjentOpplæringsinstitusjonDto(UUID.randomUUID(), "Sykehuset Telemark", gyldigePerioder),
                GodkjentOpplæringsinstitusjonDto(UUID.randomUUID(), "Sykehuset Østfold", gyldigePerioder),
                GodkjentOpplæringsinstitusjonDto(UUID.randomUUID(), "Sørlandet sykehus, Arendal", gyldigePerioder),
                GodkjentOpplæringsinstitusjonDto(UUID.randomUUID(), "Sørlandet sykehus, Farsund", gyldigePerioder),
                GodkjentOpplæringsinstitusjonDto(UUID.randomUUID(), "Sørlandet sykehus, Kristiansand", gyldigePerioder),
                GodkjentOpplæringsinstitusjonDto(UUID.randomUUID(), "Sørlandet sykehus, Mandal", gyldigePerioder),
                GodkjentOpplæringsinstitusjonDto(UUID.randomUUID(), "St. Olavs Hospital", gyldigePerioder),
                GodkjentOpplæringsinstitusjonDto(UUID.randomUUID(), "Stavanger Universitetssykehus", gyldigePerioder),
                GodkjentOpplæringsinstitusjonDto(UUID.randomUUID(), "Sunnaas sykehus", gyldigePerioder),
            ),
            second = null
        )
    }
}
