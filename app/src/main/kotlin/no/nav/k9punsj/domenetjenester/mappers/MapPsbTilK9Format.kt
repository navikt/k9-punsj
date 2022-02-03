package no.nav.k9punsj.domenetjenester.mappers

import no.nav.fpsak.tidsserie.LocalDateInterval
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Feil
import no.nav.k9.søknad.felles.opptjening.Frilanser
import no.nav.k9.søknad.felles.opptjening.OpptjeningAktivitet
import no.nav.k9.søknad.felles.opptjening.SelvstendigNæringsdrivende
import no.nav.k9.søknad.felles.personopplysninger.Barn
import no.nav.k9.søknad.felles.personopplysninger.Bosteder
import no.nav.k9.søknad.felles.personopplysninger.Søker
import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold
import no.nav.k9.søknad.felles.type.*
import no.nav.k9.søknad.ytelse.psb.v1.*
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.Arbeidstid
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.ArbeidstidInfo
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.ArbeidstidPeriodeInfo
import no.nav.k9.søknad.ytelse.psb.v1.tilsyn.TilsynPeriodeInfo
import no.nav.k9.søknad.ytelse.psb.v1.tilsyn.Tilsynsordning
import no.nav.k9punsj.rest.web.JournalpostId
import no.nav.k9punsj.rest.web.dto.PeriodeDto
import no.nav.k9punsj.rest.web.dto.PleiepengerSyktBarnSøknadDto
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

internal class MapPsbTilK9Format(
    søknadId: String,
    journalpostIder: Set<String>,
    perioderSomFinnesIK9: List<PeriodeDto>,
    dto: PleiepengerSyktBarnSøknadDto,
) {

    private val søknad = Søknad()
    private val pleiepengerSyktBarn = PleiepengerSyktBarn()
    private val feil = mutableListOf<Feil>()

    init { kotlin.runCatching {
        søknadId.leggTilSøknadId()
        Versjon.leggTilVersjon()
        dto.leggTilMottattDato()
        dto.soekerId?.leggTilSøker()
        dto.leggTilJournalposter(journalpostIder = journalpostIder)
        dto.barn?.leggTilBarn()
        dto.soeknadsperiode?.leggTilSøknadsperiode()
        dto.trekkKravPerioder.leggTilTrekkKravPerioder()
        dto.uttak.leggTilUttak(søknadsperiode = dto.soeknadsperiode)
        dto.leggTilLovestemtFerie()
        dto.beredskap?.leggTilBeredskap()
        dto.nattevaak?.leggTilNattevåk()
        dto.bosteder?.leggTilBosteder()
        if (dto.utenlandsoppholdV2.isNotEmpty()) {
            dto.utenlandsoppholdV2.leggTilUtenlandsoppholdV2()
        } else {
            dto.utenlandsopphold?.leggTilUtenlandsopphold()
        }
        dto.omsorg?.leggTilOmsorg()
        dto.opptjeningAktivitet?.leggTilOpptjeningAktivitet()
        dto.arbeidstid?.leggTilArbeidstid()
        dto.soknadsinfo?.leggTilDataBruktTilUtledning()
        dto.tilsynsordning?.perioder?.leggTilTilsynsordning()
        dto.leggTilBegrunnelseForInnsending()

        // Fullfører søknad & validerer
        søknad.medYtelse(pleiepengerSyktBarn)
        feil.addAll(Validator.valider(søknad, perioderSomFinnesIK9.somK9Perioder()))
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

    private fun PleiepengerSyktBarnSøknadDto.leggTilMottattDato() { if (mottattDato != null && klokkeslett != null) {
        søknad.medMottattDato(ZonedDateTime.of(mottattDato, klokkeslett, Oslo))
    }}

    private fun List<PeriodeDto>.leggTilSøknadsperiode() { if (this.isNotEmpty()) {
        pleiepengerSyktBarn.medSøknadsperiode(this.somK9Perioder())
    }}

    private fun PleiepengerSyktBarnSøknadDto.BarnDto.leggTilBarn() {
        val barn = Barn()
        when {
            norskIdent.erSatt() -> barn.medNorskIdentitetsnummer(NorskIdentitetsnummer.of(norskIdent))
            foedselsdato != null -> barn.medFødselsdato(foedselsdato)
        }
        pleiepengerSyktBarn.medBarn(barn)
    }

    private fun String.leggTilSøker() { if (erSatt()) {
        søknad.medSøker(Søker(NorskIdentitetsnummer.of(this)))
    }}

    private fun List<PleiepengerSyktBarnSøknadDto.BostederDto>.leggTilBosteder() {
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
            pleiepengerSyktBarn.medBosteder(Bosteder().medPerioder(k9Bosteder))
        }
    }

    private fun List<PleiepengerSyktBarnSøknadDto.UtenlandsoppholdDto>.leggTilUtenlandsopphold() {
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
            pleiepengerSyktBarn.medUtenlandsopphold(Utenlandsopphold().medPerioder(k9Utenlandsopphold))
        }
    }

    private fun List<PleiepengerSyktBarnSøknadDto.UtenlandsoppholdDtoV2>.leggTilUtenlandsoppholdV2() {
        val k9Utenlandsopphold = mutableMapOf<Periode, Utenlandsopphold.UtenlandsoppholdPeriodeInfo>()
        filter { it.periode.erSatt() }.forEach { utenlandsopphold ->

            val oppholdet = LocalDateSegment(utenlandsopphold.periode!!.fom, utenlandsopphold.periode.tom, INGEN_ÅRSAK)
            val innleggelsesperioder = LocalDateTimeline(utenlandsopphold.innleggelsesperioder.map {
                LocalDateSegment(it.periode!!.fom, it.periode.tom, it.årsak!!)
            })

            val utenlandsoppholdet = LocalDateTimeline(listOf(oppholdet))
            val listeMedAllePerioder = utenlandsoppholdet.combine(innleggelsesperioder,
                { interval, overlappenePeriode, innleggelsesperiode -> slåSammen(interval, overlappenePeriode, innleggelsesperiode) }
                , LocalDateTimeline.JoinStyle.CROSS_JOIN)

            val intervaller = listeMedAllePerioder.localDateIntervals

            intervaller.forEach{ interval ->
                val segment = listeMedAllePerioder.getSegment(interval)
                val k9Periode = segment.localDateInterval.somK9Periode()
                val k9Info = Utenlandsopphold.UtenlandsoppholdPeriodeInfo()
                if (utenlandsopphold.land.erSatt()) {
                    k9Info.medLand(Landkode.of(utenlandsopphold.land))
                }
                if (segment.value != INGEN_ÅRSAK) {
                    mapEllerLeggTilFeil("ytelse.utenlandsopphold.${k9Periode.jsonPath()}.årsak") {
                        Utenlandsopphold.UtenlandsoppholdÅrsak.of(segment.value)
                    }?.also { k9Info.medÅrsak(it) }
                }
                k9Utenlandsopphold[k9Periode] = k9Info
            }
        }
        if (k9Utenlandsopphold.isNotEmpty()) {
            pleiepengerSyktBarn.medUtenlandsopphold(Utenlandsopphold().medPerioder(k9Utenlandsopphold))
        }
    }

    private fun slåSammen(intervall: LocalDateInterval, utenlandsoppholdet: LocalDateSegment<String>?, innleggelsesperiode: LocalDateSegment<String>?): LocalDateSegment<String> {
        if (utenlandsoppholdet != null && innleggelsesperiode != null) {
            return LocalDateSegment(intervall, innleggelsesperiode.value)
        } else if (innleggelsesperiode != null) {
            return LocalDateSegment(intervall, innleggelsesperiode.value)
        }
        return LocalDateSegment(intervall, "")
    }

    private fun List<PleiepengerSyktBarnSøknadDto.NattevåkDto>.leggTilNattevåk() {
        val k9Nattevåk = mutableMapOf<Periode, Nattevåk.NattevåkPeriodeInfo>()
        filter { it.periode.erSatt() }.forEach { nattevåk ->
            val k9Periode = nattevåk.periode!!.somK9Periode()!!
            val k9Info = Nattevåk.NattevåkPeriodeInfo()
            if (nattevåk.tilleggsinformasjon.erSatt()) {
                k9Info.medTilleggsinformasjon(nattevåk.tilleggsinformasjon)
            }
            k9Nattevåk[k9Periode] = k9Info
        }
        if (k9Nattevåk.isNotEmpty()) {
            pleiepengerSyktBarn.medNattevåk(Nattevåk().medPerioder(k9Nattevåk))
        }
    }

    private fun List<PleiepengerSyktBarnSøknadDto.BeredskapDto>.leggTilBeredskap() {
        val k9Beredskap = mutableMapOf<Periode, Beredskap.BeredskapPeriodeInfo>()
        filter { it.periode.erSatt() }.forEach { beredskap ->
            val k9Periode = beredskap.periode!!.somK9Periode()!!
            val k9Info = Beredskap.BeredskapPeriodeInfo()
            if (beredskap.tilleggsinformasjon.erSatt()) {
                k9Info.medTilleggsinformasjon(beredskap.tilleggsinformasjon)
            }
            k9Beredskap[k9Periode] = k9Info
        }
        if (k9Beredskap.isNotEmpty()) {
            pleiepengerSyktBarn.medBeredskap(Beredskap().medPerioder(k9Beredskap))
        }
    }

    private fun List<PleiepengerSyktBarnSøknadDto.UttakDto>?.leggTilUttak(søknadsperiode: List<PeriodeDto>?) {
        val k9Uttak = mutableMapOf<Periode, Uttak.UttakPeriodeInfo>()

        this?.filter { it.periode.erSatt() }?.forEach { uttak ->
            val k9Periode = uttak.periode!!.somK9Periode()!!
            val k9Info = Uttak.UttakPeriodeInfo()
            mapEllerLeggTilFeil("ytelse.uttak.perioder.${k9Periode.jsonPath()}.timerPleieAvBarnetPerDag")
            { uttak.pleieAvBarnetPerDag?.somDuration() }?.also {
                k9Info.medTimerPleieAvBarnetPerDag(it)
            }
            k9Uttak[k9Periode] = k9Info
        }

        if (k9Uttak.isEmpty() && søknadsperiode != null) {
            søknadsperiode.forEach {
                k9Uttak[it.somK9Periode()!!] = DefaultUttak
            }
        }

        if (k9Uttak.isNotEmpty()) {
            pleiepengerSyktBarn.medUttak(Uttak().medPerioder(k9Uttak))
        }
    }

    private fun PleiepengerSyktBarnSøknadDto.leggTilLovestemtFerie() {
        if (lovbestemtFerie.isNullOrEmpty() && lovbestemtFerieSomSkalSlettes.isNullOrEmpty()) {
            return
        }
        val k9LovbestemtFerie = mutableMapOf<Periode, LovbestemtFerie.LovbestemtFeriePeriodeInfo>()
        lovbestemtFerie?.filter { it.erSatt() }?.forEach { periode ->
            k9LovbestemtFerie[periode.somK9Periode()!!] = LovbestemtFerie.LovbestemtFeriePeriodeInfo().medSkalHaFerie(true)
        }
        lovbestemtFerieSomSkalSlettes?.filter { it.erSatt() }?.forEach { periode ->
            k9LovbestemtFerie[periode.somK9Periode()!!] = LovbestemtFerie.LovbestemtFeriePeriodeInfo().medSkalHaFerie(false)
        }
        pleiepengerSyktBarn.medLovbestemtFerie(LovbestemtFerie().medPerioder(k9LovbestemtFerie))
    }

    private fun Set<PeriodeDto>.leggTilTrekkKravPerioder() {
        pleiepengerSyktBarn.addAllTrekkKravPerioder(this.somK9Perioder())
    }

    private fun PleiepengerSyktBarnSøknadDto.leggTilBegrunnelseForInnsending() {
        if(begrunnelseForInnsending != null) {
            søknad.medBegrunnelseForInnsending(begrunnelseForInnsending)
        }
    }

    private fun PleiepengerSyktBarnSøknadDto.leggTilJournalposter(journalpostIder: Set<JournalpostId>) {
        journalpostIder.forEach { journalpostId ->
            søknad.medJournalpost(Journalpost()
                .medJournalpostId(journalpostId)
                .medInformasjonSomIkkeKanPunsjes(harInfoSomIkkeKanPunsjes)
                .medInneholderMedisinskeOpplysninger(harMedisinskeOpplysninger)
            )
        }
    }

    private fun PleiepengerSyktBarnSøknadDto.OmsorgDto.leggTilOmsorg() {
        val k9Omsorg = Omsorg()
        mapEllerLeggTilFeil("ytelse.omsorg.relasjonTilBarnet") {
            relasjonTilBarnet?.blankAsNull()?.let { Omsorg.BarnRelasjon.valueOf(it.uppercase()) }
        }?.also { k9Omsorg.medRelasjonTilBarnet(it) }

        if (beskrivelseAvOmsorgsrollen.erSatt()) {
            k9Omsorg.medBeskrivelseAvOmsorgsrollen(beskrivelseAvOmsorgsrollen!!)
        }
        pleiepengerSyktBarn.medOmsorg(k9Omsorg)
    }

    private fun PleiepengerSyktBarnSøknadDto.ArbeidAktivitetDto.leggTilOpptjeningAktivitet() {
        val k9OpptjeningAktivitet = OpptjeningAktivitet()
        selvstendigNaeringsdrivende?.mapOpptjeningAktivitetSelvstendigNæringsdrivende()?.also { k9OpptjeningAktivitet.medSelvstendigNæringsdrivende(it) }
        frilanser?.also { k9OpptjeningAktivitet.medFrilanser(it.mapOpptjeningAktivitetFrilanser()) }
        pleiepengerSyktBarn.medOpptjeningAktivitet(k9OpptjeningAktivitet)
    }

    private fun PleiepengerSyktBarnSøknadDto.ArbeidAktivitetDto.SelvstendigNæringsdrivendeDto.mapOpptjeningAktivitetSelvstendigNæringsdrivende() : SelvstendigNæringsdrivende? {
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

    private fun PleiepengerSyktBarnSøknadDto.ArbeidAktivitetDto.FrilanserDto.mapOpptjeningAktivitetFrilanser() : Frilanser {
        val k9Frilanser = Frilanser()
        if (startdato.erSatt()) mapEllerLeggTilFeil("ytelse.opptjening.frilanser.startDato") { LocalDate.parse(startdato) }?.also {
            k9Frilanser.medStartDato(it)
        }
        if (sluttdato.erSatt()) mapEllerLeggTilFeil("ytelse.opptjening.frilanser.sluttDato") { LocalDate.parse(sluttdato) }?.also {
            k9Frilanser.medSluttDato(it)
        }
        return k9Frilanser
    }

    private fun PleiepengerSyktBarnSøknadDto.ArbeidstidDto.leggTilArbeidstid() {
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
        pleiepengerSyktBarn.medArbeidstid(k9Arbeidstid)
    }

    private fun List<PleiepengerSyktBarnSøknadDto.ArbeidAktivitetDto.ArbeidstakerDto>.mapArbeidstidArbeidstaker() = mapIndexedNotNull { index, arbeidstaker ->
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

    private fun PleiepengerSyktBarnSøknadDto.ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto.mapArbeidstid(type: String) : ArbeidstidInfo? {
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

    private fun PleiepengerSyktBarnSøknadDto.DataBruktTilUtledningDto.leggTilDataBruktTilUtledning() {
        val k9DataBruktTilUtledning = DataBruktTilUtledning()
        samtidigHjemme?.also { k9DataBruktTilUtledning.medSamtidigHjemme(it) }
        harMedsoeker?.also { k9DataBruktTilUtledning.medHarMedsøker(it) }
        pleiepengerSyktBarn.medSøknadInfo(k9DataBruktTilUtledning)
    }

    private fun List<PleiepengerSyktBarnSøknadDto.TilsynsordningInfoDto>.leggTilTilsynsordning() {
        val k9Tilsynsordning = mutableMapOf<Periode, TilsynPeriodeInfo>()
        filter { it.periode.erSatt() }.forEach { tilsynsordning ->
            val k9Periode = tilsynsordning.periode!!.somK9Periode()!!
            k9Tilsynsordning[k9Periode] = TilsynPeriodeInfo()
                .medEtablertTilsynTimerPerDag(Duration
                    .ofHours(tilsynsordning.timer.toLong())
                    .plusMinutes(tilsynsordning.minutter.toLong()
                ))
        }
        if (k9Tilsynsordning.isNotEmpty()) {
            pleiepengerSyktBarn.medTilsynsordning(Tilsynsordning().medPerioder(k9Tilsynsordning))
        }
    }

    private fun <Til>mapEllerLeggTilFeil(felt: String, map: () -> Til?) = kotlin.runCatching {
        map()
    }.fold(onSuccess = {it}, onFailure = { throwable ->
        feil.add(Feil(felt, throwable.javaClass.simpleName, throwable.message ?: "Ingen feilmelding"))
        null
    })

    internal companion object {
        private val logger = LoggerFactory.getLogger(MapPsbTilK9Format::class.java)
        private val Oslo = ZoneId.of("Europe/Oslo")
        private val Validator = PleiepengerSyktBarnSøknadValidator()
        private const val Versjon = "1.0.0"
        private const val INGEN_ÅRSAK = ""
        private val DefaultUttak = Uttak.UttakPeriodeInfo().medTimerPleieAvBarnetPerDag(Duration.ofHours(7).plusMinutes(30))
        private fun PeriodeDto?.erSatt() = this != null && (fom != null || tom != null)
        private fun PeriodeDto.somK9Periode() = when (erSatt()) {
            true -> Periode(fom, tom)
            else -> null
        }
        private fun LocalDateInterval.somK9Periode() : Periode {
            return Periode(this.fomDato, this.tomDato)
        }

        private fun Collection<PeriodeDto>.somK9Perioder() = mapNotNull { it.somK9Periode() }
        private fun String?.erSatt() = !isNullOrBlank()
        private fun String.blankAsNull() = when (isBlank()) {
            true -> null
            false -> this
        }

        private fun Periode.jsonPath() = "[${this.iso8601}]"
        private fun PleiepengerSyktBarnSøknadDto.TimerOgMinutter.somDuration() = Duration.ofHours(timer).plusMinutes(minutter.toLong())
    }
}
