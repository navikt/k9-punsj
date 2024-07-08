package no.nav.k9punsj.felles.k9format

import no.nav.k9.søknad.felles.Feil
import no.nav.k9.søknad.felles.opptjening.Frilanser
import no.nav.k9.søknad.felles.opptjening.OpptjeningAktivitet
import no.nav.k9.søknad.felles.opptjening.SelvstendigNæringsdrivende
import no.nav.k9.søknad.felles.type.Landkode
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.Organisasjonsnummer
import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9.søknad.felles.type.VirksomhetType
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.ArbeidstidInfo
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.ArbeidstidPeriodeInfo
import no.nav.k9punsj.felles.ZoneUtils.Oslo
import no.nav.k9punsj.felles.dto.ArbeidAktivitetDto
import no.nav.k9punsj.felles.dto.TimerOgMinutter.Companion.somDuration
import no.nav.k9punsj.felles.k9format.MappingUtils.mapEllerLeggTilFeil
import no.nav.k9punsj.utils.PeriodeUtils.erSatt
import no.nav.k9punsj.utils.PeriodeUtils.jsonPath
import no.nav.k9punsj.utils.PeriodeUtils.somK9Periode
import no.nav.k9punsj.utils.StringUtils.blankAsNull
import no.nav.k9punsj.utils.StringUtils.erSatt
import java.time.LocalDate

fun ArbeidAktivitetDto.mapOpptjeningAktivitet(feil: MutableList<Feil>): OpptjeningAktivitet {
    val k9OpptjeningAktivitet = OpptjeningAktivitet()
    selvstendigNaeringsdrivende?.mapOpptjeningAktivitetSelvstendigNæringsdrivende(feil)
        ?.also { k9OpptjeningAktivitet.medSelvstendigNæringsdrivende(it) }

    if (frilanser != null) frilanser.also { k9OpptjeningAktivitet.medFrilanser(it.mapOpptjeningAktivitetFrilanser(feil)) }
    else k9OpptjeningAktivitet.medFrilanser(Frilanser()) // Fjerner frilansinfo fra k9-sak.

    return k9OpptjeningAktivitet
}

fun ArbeidAktivitetDto.SelvstendigNæringsdrivendeDto.mapOpptjeningAktivitetSelvstendigNæringsdrivende(feil: MutableList<Feil>): SelvstendigNæringsdrivende? {
    val noeSatt = organisasjonsnummer.erSatt() || virksomhetNavn.erSatt() || info?.periode.erSatt()
    if (!noeSatt) return null

    val k9SelvstendigNæringsdrivende = SelvstendigNæringsdrivende()
    if (organisasjonsnummer.erSatt()) k9SelvstendigNæringsdrivende.medOrganisasjonsnummer(
        Organisasjonsnummer.of(
            organisasjonsnummer
        )
    )
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
        info.erFiskerPåBladB?.also { k9Info.medErFiskerPåBladB(it) }
        // TODO: Denne utledningen virker rar, men flagget skal forhåpentligvis fjernes fra K9-Format.
        k9Info.medErNyoppstartet(k9Periode.fraOgMed.isAfter(LocalDate.now(Oslo).minusYears(4)))
        when (info.erVarigEndring) {
            true -> info.endringInntekt
            else -> info.bruttoInntekt
        }?.also { k9Info.medBruttoInntekt(it) }

        if (!info.virksomhetstyper.isNullOrEmpty()) {
            val k9Virksomhetstyper = info.virksomhetstyper.mapIndexedNotNull { index, virksomhetstype ->
                when {
                    virksomhetstype.isBlank() -> null
                    virksomhetstype.lowercase().contains("dagmamma") -> VirksomhetType.DAGMAMMA
                    virksomhetstype.lowercase().contains("fiske") -> VirksomhetType.FISKE
                    virksomhetstype.lowercase().contains("jordbruk") -> VirksomhetType.JORDBRUK_SKOGBRUK
                    virksomhetstype.lowercase().contains("annen") -> VirksomhetType.ANNEN
                    else -> mapEllerLeggTilFeil(feil, "ytelse.opptjening.selvstendigNæringsdrivende.${k9Periode.jsonPath()}.virksomhetstyper[$index]") {
                        VirksomhetType.valueOf(virksomhetstype.uppercase())
                    }
                }
            }
            k9Info.medVirksomhetstyper(k9Virksomhetstyper)
        }
        k9SelvstendigNæringsdrivende.medPerioder(mutableMapOf(k9Periode to k9Info))
    }
    return k9SelvstendigNæringsdrivende
}

fun ArbeidAktivitetDto.FrilanserDto.mapOpptjeningAktivitetFrilanser(feil: MutableList<Feil>): Frilanser {
    val k9Frilanser = Frilanser()
    if (startdato.erSatt()) mapEllerLeggTilFeil(feil, "ytelse.opptjening.frilanser.startDato") { LocalDate.parse(startdato) }?.also {
        k9Frilanser.medStartdato(it)
    }
    if (sluttdato.erSatt()) mapEllerLeggTilFeil(feil, "ytelse.opptjening.frilanser.sluttDato") { LocalDate.parse(sluttdato) }?.also {
        k9Frilanser.medSluttdato(it)
    }
    return k9Frilanser
}

fun List<ArbeidAktivitetDto.ArbeidstakerDto>.mapArbeidstidArbeidstaker(feil: MutableList<Feil>) =
    mapIndexedNotNull { index, arbeidstaker ->
        val k9Arbeidstaker = no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.Arbeidstaker()
        if (arbeidstaker.norskIdent.erSatt()) {
            k9Arbeidstaker.medNorskIdentitetsnummer(NorskIdentitetsnummer.of(arbeidstaker.norskIdent))
        }
        if (arbeidstaker.organisasjonsnummer.erSatt()) {
            k9Arbeidstaker.medOrganisasjonsnummer(Organisasjonsnummer.of(arbeidstaker.organisasjonsnummer))
        }
        arbeidstaker.arbeidstidInfo?.mapArbeidstid("arbeidstakerList[$index]", feil)
            ?.let { k9Arbeidstaker.medArbeidstidInfo(it) }

        val noeSatt =
            arbeidstaker.norskIdent.erSatt() || arbeidstaker.organisasjonsnummer.erSatt() || k9Arbeidstaker.arbeidstidInfo != null
        if (noeSatt) {
            k9Arbeidstaker
        } else {
            null
        }
    }

fun ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto.mapArbeidstid(
    type: String,
    feil: MutableList<Feil>
): ArbeidstidInfo? {
    val k9ArbeidstidPeriodeInfo = mutableMapOf<Periode, ArbeidstidPeriodeInfo>()
    this.perioder?.filter { it.periode.erSatt() }?.forEach { periode ->
        val k9Periode = periode.periode!!.somK9Periode()!!
        val k9Info = ArbeidstidPeriodeInfo()
        val felt = "ytelse.arbeisdtid.$type.arbeidstidInfo.perioder.${k9Periode.jsonPath()}"
        mapEllerLeggTilFeil(feil, "$felt.faktiskArbeidTimerPerDag") {
            periode.faktiskArbeidPerDag?.somDuration()
        }?.also { k9Info.medFaktiskArbeidTimerPerDag(it) }
        mapEllerLeggTilFeil(feil, "$felt.jobberNormaltTimerPerDag") {
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
