package no.nav.k9punsj.domenetjenester.mappers

import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Feil
import no.nav.k9.søknad.felles.opptjening.Frilanser
import no.nav.k9.søknad.felles.opptjening.OpptjeningAktivitet
import no.nav.k9.søknad.felles.opptjening.SelvstendigNæringsdrivende
import no.nav.k9.søknad.felles.personopplysninger.Bosteder
import no.nav.k9.søknad.felles.personopplysninger.Søker
import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold
import no.nav.k9.søknad.felles.type.*
import no.nav.k9.søknad.ytelse.pls.v1.PleiepengerLivetsSluttfaseSøknadValidator
import no.nav.k9.søknad.ytelse.pls.v1.Pleietrengende
import no.nav.k9.søknad.ytelse.pls.v1.PleipengerLivetsSluttfase
import no.nav.k9.søknad.ytelse.psb.v1.Uttak
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.Arbeidstid
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.ArbeidstidInfo
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.ArbeidstidPeriodeInfo
import no.nav.k9punsj.rest.web.JournalpostId
import no.nav.k9punsj.rest.web.dto.PeriodeDto
import no.nav.k9punsj.rest.web.dto.PleiepengerLivetsSluttfaseSøknadDto
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

internal class MapPlsfTilK9Format(
    søknadId: String,
    journalpostIder: Set<String>,
    dto: PleiepengerLivetsSluttfaseSøknadDto
) {

    private val søknad = Søknad()
    private val pleipengerLivetsSluttfase = PleipengerLivetsSluttfase()
    private val feil = mutableListOf<Feil>()

    init { kotlin.runCatching {
        søknadId.leggTilSøknadId()
        Versjon.leggTilVersjon()
        dto.leggTilMottattDato()
        dto.soekerId?.leggTilSøker()
        dto.leggTilJournalposter(journalpostIder = journalpostIder)
        dto.pleietrengende?.leggTilPleietrengende()
        dto.bosteder?.leggTilBosteder()
        dto.utenlandsopphold?.leggTilUtenlandsopphold()
        dto.opptjeningAktivitet?.leggTilOpptjeningAktivitet()
        dto.arbeidstid?.leggTilArbeidstid()
        dto.trekkKravPerioder.leggTilTrekkKravPerioder()
        dto.leggTilBegrunnelseForInnsending()

        // Fullfører søknad & validerer
        søknad.medYtelse(pleipengerLivetsSluttfase)
        feil.addAll(Validator.valider(søknad))
    }.onFailure { throwable ->
        logger.error("Uventet mappingfeil", throwable)
        feil.add(Feil("søknad", "uventetMappingfeil", throwable.message ?: "Uventet mappingfeil"))
    }}

    internal fun søknad() = søknad
    internal fun feil() = feil.toList()
    internal fun søknadOgFeil() = søknad() to feil()

    private fun String.leggTilSøknadId() { if (erSatt()) {
        søknad.medSøknadId(this)
    }}

    private fun String.leggTilVersjon() {
        søknad.medVersjon(this)
    }

    private fun PleiepengerLivetsSluttfaseSøknadDto.leggTilMottattDato() { if (mottattDato != null && klokkeslett != null) {
        søknad.medMottattDato(ZonedDateTime.of(mottattDato, klokkeslett, Oslo))
    }}

    private fun PleiepengerLivetsSluttfaseSøknadDto.PleietrengendeDto.leggTilPleietrengende() {
        pleipengerLivetsSluttfase.medPleietrengende(Pleietrengende(NorskIdentitetsnummer.of(this.norskIdent)))
    }

    private fun String.leggTilSøker() { if (erSatt()) {
        søknad.medSøker(Søker(NorskIdentitetsnummer.of(this)))
    }}

    private fun List<PleiepengerLivetsSluttfaseSøknadDto.BostederDto>.leggTilBosteder() {
        val k9Bosteder = mutableMapOf<Periode, Bosteder.BostedPeriodeInfo>()
        filter { it.periode.erSatt() }.forEach { bosted ->
            val k9Periode = bosted.periode!!.somK9Periode()!!
            val k9Info = Bosteder.BostedPeriodeInfo()
            if (bosted.land.erSatt()) {
                k9Info.medLand(Landkode.of(bosted.land))
            }
            k9Bosteder[k9Periode] = k9Info
        }
        if (k9Bosteder.isNotEmpty()) {
            pleipengerLivetsSluttfase.medBosteder(Bosteder().medPerioder(k9Bosteder))
        }
    }

    private fun List<PleiepengerLivetsSluttfaseSøknadDto.UtenlandsoppholdDto>.leggTilUtenlandsopphold() {
        val k9Utenlandsopphold = mutableMapOf<Periode, Utenlandsopphold.UtenlandsoppholdPeriodeInfo>()
        filter { it.periode.erSatt() }.forEach { utenlandsopphold ->
            val k9Periode = utenlandsopphold.periode!!.somK9Periode()!!
            val k9Info = Utenlandsopphold.UtenlandsoppholdPeriodeInfo()
            if (utenlandsopphold.land.erSatt()) {
                k9Info.medLand(Landkode.of(utenlandsopphold.land))
            }
            if (utenlandsopphold.årsak.erSatt()) {
                mapEllerLeggTilFeil("ytelse.utenlandsopphold.${k9Periode.jsonPath()}.årsak") {
                    Utenlandsopphold.UtenlandsoppholdÅrsak.of(utenlandsopphold.årsak!!)
                }?.also { k9Info.medÅrsak(it) }
            }

            k9Utenlandsopphold[k9Periode] = k9Info
        }
        if (k9Utenlandsopphold.isNotEmpty()) {
            pleipengerLivetsSluttfase.medUtenlandsopphold(Utenlandsopphold().medPerioder(k9Utenlandsopphold))
        }
    }



    private fun PleiepengerLivetsSluttfaseSøknadDto.leggTilBegrunnelseForInnsending() {
        if(begrunnelseForInnsending != null) {
            søknad.medBegrunnelseForInnsending(begrunnelseForInnsending)
        }
    }

    private fun PleiepengerLivetsSluttfaseSøknadDto.leggTilJournalposter(journalpostIder: Set<JournalpostId>) {
        journalpostIder.forEach { journalpostId ->
            søknad.medJournalpost(Journalpost()
                .medJournalpostId(journalpostId)
                .medInformasjonSomIkkeKanPunsjes(harInfoSomIkkeKanPunsjes)
                .medInneholderMedisinskeOpplysninger(harMedisinskeOpplysninger)
            )
        }
    }

    private fun PleiepengerLivetsSluttfaseSøknadDto.ArbeidAktivitetDto.leggTilOpptjeningAktivitet() {
        val k9OpptjeningAktivitet = OpptjeningAktivitet()
        selvstendigNaeringsdrivende?.mapOpptjeningAktivitetSelvstendigNæringsdrivende()?.also { k9OpptjeningAktivitet.medSelvstendigNæringsdrivende(it) }
        frilanser?.also { k9OpptjeningAktivitet.medFrilanser(it.mapOpptjeningAktivitetFrilanser()) }
        pleipengerLivetsSluttfase.medOpptjeningAktivitet(k9OpptjeningAktivitet)
    }

    private fun PleiepengerLivetsSluttfaseSøknadDto.ArbeidAktivitetDto.SelvstendigNæringsdrivendeDto.mapOpptjeningAktivitetSelvstendigNæringsdrivende() : SelvstendigNæringsdrivende? {
        val noeSatt = organisasjonsnummer.erSatt() || virksomhetNavn.erSatt() || info?.periode.erSatt()
        if (!noeSatt) return null

        val k9SelvstendigNæringsdrivende = SelvstendigNæringsdrivende()
        if (organisasjonsnummer.erSatt()) k9SelvstendigNæringsdrivende.medOrganisasjonsnummer(Organisasjonsnummer.of(organisasjonsnummer))
        if (virksomhetNavn.erSatt()) k9SelvstendigNæringsdrivende.medVirksomhetNavn(virksomhetNavn)

        if (info?.periode.erSatt()) {
            val k9Periode = info!!.periode!!.somK9Periode()!!
            val k9Info = SelvstendigNæringsdrivende.SelvstendigNæringsdrivendePeriodeInfo()
            info.registrertIUtlandet?.also { k9Info.medRegistrertIUtlandet(it) }
            info.regnskapsførerNavn?.blankAsNull()?.also { k9Info.medRegnskapsførerNavn(it) }
            info.regnskapsførerTlf?.blankAsNull()?.also { k9Info.medRegnskapsførerTlf(it) }
            info.landkode?.blankAsNull()?.also { k9Info.medLandkode(Landkode.of(it)) }
            info.bruttoInntekt?.also { k9Info.medBruttoInntekt(it) }
            info.erVarigEndring?.also { k9Info.medErVarigEndring(it) }
            info.endringDato?.also { k9Info.medEndringDato(it) }
            info.endringBegrunnelse?.blankAsNull()?.also { k9Info.medEndringBegrunnelse(it) }
            // TODO: Denne utledningen virker rar, men flagget skal forhåpentligvis fjernes fra K9-Format.
            k9Info.medErNyoppstartet(k9Periode.fraOgMed.isAfter(LocalDate.now(Oslo).minusYears(4)))
            when (info.erVarigEndring) {
                true -> info.endringInntekt
                else -> info.bruttoInntekt
            }?.also { k9Info.medBruttoInntekt(it) }

            if (!info.virksomhetstyper.isNullOrEmpty()) {
                val k9Virksomhetstyper = info.virksomhetstyper.mapIndexedNotNull { index, virksomhetstype -> when {
                    virksomhetstype.isBlank() -> null
                    virksomhetstype.lowercase().contains("dagmamma") -> VirksomhetType.DAGMAMMA
                    virksomhetstype.lowercase().contains("fiske") -> VirksomhetType.FISKE
                    virksomhetstype.lowercase().contains("jordbruk") -> VirksomhetType.JORDBRUK_SKOGBRUK
                    virksomhetstype.lowercase().contains("annen") -> VirksomhetType.ANNEN
                    else -> mapEllerLeggTilFeil("ytelse.opptjening.selvstendigNæringsdrivende.${k9Periode.jsonPath()}.virksomhetstyper[$index]") {
                        VirksomhetType.valueOf(virksomhetstype.uppercase())
                    }
                }}
                k9Info.medVirksomhetstyper(k9Virksomhetstyper)
            }
            k9SelvstendigNæringsdrivende.medPerioder(mutableMapOf(k9Periode to k9Info))
        }
        return k9SelvstendigNæringsdrivende
    }

    private fun PleiepengerLivetsSluttfaseSøknadDto.ArbeidAktivitetDto.FrilanserDto.mapOpptjeningAktivitetFrilanser() : Frilanser {
        val k9Frilanser = Frilanser()
        if (startdato.erSatt()) mapEllerLeggTilFeil("ytelse.opptjening.frilanser.startDato") { LocalDate.parse(startdato) }?.also {
            k9Frilanser.medStartDato(it)
        }
        if (sluttdato.erSatt()) mapEllerLeggTilFeil("ytelse.opptjening.frilanser.sluttDato") { LocalDate.parse(sluttdato) }?.also {
            k9Frilanser.medSluttDato(it)
        }
        return k9Frilanser
    }

    private fun PleiepengerLivetsSluttfaseSøknadDto.ArbeidstidDto.leggTilArbeidstid() {
        val k9Arbeidstid = Arbeidstid()
        arbeidstakerList?.also {
            k9Arbeidstid.medArbeidstaker(it.mapArbeidstidArbeidstaker())
        }
        selvstendigNæringsdrivendeArbeidstidInfo?.mapArbeidstid("selvstendigNæringsdrivendeArbeidstidInfo")?.also {
            k9Arbeidstid.medSelvstendigNæringsdrivendeArbeidstidInfo(it)
        }
        frilanserArbeidstidInfo?.mapArbeidstid("frilanserArbeidstidInfo")?.also {
            k9Arbeidstid.medFrilanserArbeidstid(it)
        }
        pleipengerLivetsSluttfase.medArbeidstid(k9Arbeidstid)
    }

    private fun List<PleiepengerLivetsSluttfaseSøknadDto.ArbeidAktivitetDto.ArbeidstakerDto>.mapArbeidstidArbeidstaker() = mapIndexedNotNull { index, arbeidstaker ->
        val k9Arbeidstaker = no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.Arbeidstaker()
        if (arbeidstaker.norskIdent.erSatt()) {
            k9Arbeidstaker.medNorskIdentitetsnummer(NorskIdentitetsnummer.of(arbeidstaker.norskIdent))
        }
        if (arbeidstaker.organisasjonsnummer.erSatt()) {
            k9Arbeidstaker.medOrganisasjonsnummer(Organisasjonsnummer.of(arbeidstaker.organisasjonsnummer))
        }
        arbeidstaker.arbeidstidInfo?.mapArbeidstid("arbeidstakerList[$index]")?.let { k9Arbeidstaker.medArbeidstidInfo(it) }

        val noeSatt = arbeidstaker.norskIdent.erSatt() || arbeidstaker.organisasjonsnummer.erSatt() || k9Arbeidstaker.arbeidstidInfo != null
        if (noeSatt) {
            k9Arbeidstaker
        } else {
            null
        }
    }

    private fun PleiepengerLivetsSluttfaseSøknadDto.ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto.mapArbeidstid(type: String) : ArbeidstidInfo? {
        val k9ArbeidstidPeriodeInfo = mutableMapOf<Periode, ArbeidstidPeriodeInfo>()
        this.perioder?.filter { it.periode.erSatt() }?.forEach{ periode ->
            val k9Periode = periode.periode!!.somK9Periode()!!
            val k9Info = ArbeidstidPeriodeInfo()
            val felt = "ytelse.arbeisdtid.$type.arbeidstidInfo.perioder.${k9Periode.jsonPath()}"
            mapEllerLeggTilFeil("$felt.faktiskArbeidTimerPerDag") {
                periode.faktiskArbeidPerDag?.somDuration()
            }?.also { k9Info.medFaktiskArbeidTimerPerDag(it) }
            mapEllerLeggTilFeil("$felt.jobberNormaltTimerPerDag") {
                periode.jobberNormaltPerDag?.somDuration()
            }?.also { k9Info.medJobberNormaltTimerPerDag(it) }
            k9ArbeidstidPeriodeInfo[k9Periode] = k9Info
        }
        return if (k9ArbeidstidPeriodeInfo.isNotEmpty()) {
            ArbeidstidInfo().medPerioder(k9ArbeidstidPeriodeInfo)
        } else {
            null
        }
    }

    private fun <Til>mapEllerLeggTilFeil(felt: String, map: () -> Til?) = kotlin.runCatching {
        map()
    }.fold(onSuccess = {it}, onFailure = { throwable ->
        feil.add(Feil(felt, throwable.javaClass.simpleName, throwable.message ?: "Ingen feilmelding"))
        null
    })

    private fun Set<PeriodeDto>.leggTilTrekkKravPerioder() {
        if (isNotEmpty()) {
            pleipengerLivetsSluttfase.leggTilTrekkKravPerioder(this.somK9Perioder())
        } else {
            return
        }
    }

    internal companion object {
        private val logger = LoggerFactory.getLogger(MapPlsfTilK9Format::class.java)
        private val Oslo = ZoneId.of("Europe/Oslo")
        private val Validator = PleiepengerLivetsSluttfaseSøknadValidator()
        private const val Versjon = "1.0.0"
        private val DefaultUttak = Uttak.UttakPeriodeInfo().medTimerPleieAvBarnetPerDag(Duration.ofHours(7).plusMinutes(30))
        private fun PeriodeDto?.erSatt() = this != null && (fom != null || tom != null)
        private fun PeriodeDto.somK9Periode() = when (erSatt()) {
            true -> Periode(fom, tom)
            else -> null
        }
        private fun Collection<PeriodeDto>.somK9Perioder() = mapNotNull { it.somK9Periode() }
        private fun String?.erSatt() = !isNullOrBlank()
        private fun String.blankAsNull() = when (isBlank()) {
            true -> null
            false -> this
        }

        private fun Periode.jsonPath() = "[${this.iso8601}]"
        private fun PleiepengerLivetsSluttfaseSøknadDto.TimerOgMinutter.somDuration() = Duration.ofHours(timer).plusMinutes(minutter.toLong())
    }
}
