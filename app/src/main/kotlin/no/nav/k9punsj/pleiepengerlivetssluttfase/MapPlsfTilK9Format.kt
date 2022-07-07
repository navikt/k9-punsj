package no.nav.k9punsj.pleiepengerlivetssluttfase

import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Feil
import no.nav.k9.søknad.felles.personopplysninger.Søker
import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold
import no.nav.k9.søknad.felles.type.Journalpost
import no.nav.k9.søknad.felles.type.Landkode
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9.søknad.ytelse.pls.v1.PleiepengerLivetsSluttfaseSøknadValidator
import no.nav.k9.søknad.ytelse.pls.v1.Pleietrengende
import no.nav.k9.søknad.ytelse.pls.v1.PleipengerLivetsSluttfase
import no.nav.k9.søknad.ytelse.psb.v1.Uttak
import no.nav.k9punsj.felles.DurationMapper.somDuration
import no.nav.k9punsj.felles.ZoneUtils.Oslo
import no.nav.k9punsj.felles.dto.ArbeidAktivitetDto
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.felles.dto.PleietrengendeDto
import no.nav.k9punsj.felles.dto.UtenlandsoppholdDto
import no.nav.k9punsj.felles.k9format.MappingUtils.mapEllerLeggTilFeil
import no.nav.k9punsj.felles.k9format.mapOpptjeningAktivitet
import no.nav.k9punsj.felles.k9format.mapTilArbeidstid
import no.nav.k9punsj.felles.k9format.mapTilBosteder
import no.nav.k9punsj.utils.PeriodeUtils.erSatt
import no.nav.k9punsj.utils.PeriodeUtils.jsonPath
import no.nav.k9punsj.utils.PeriodeUtils.somK9Periode
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.ZonedDateTime

internal class MapPlsfTilK9Format(
    søknadId: String,
    journalpostIder: Set<String>,
    perioderSomFinnesIK9: List<PeriodeDto>,
    dto: PleiepengerLivetsSluttfaseSøknadDto
) {

    private val søknad = Søknad()
    private val pleipengerLivetsSluttfase = PleipengerLivetsSluttfase()
    private val feil = mutableListOf<Feil>()

    init {
        kotlin.runCatching {
            søknadId.leggTilSøknadId()
            Versjon.leggTilVersjon()
            dto.leggTilMottattDatoOgKlokkeslett()
            dto.soekerId?.leggTilSøker()
            dto.soeknadsperiode?.leggTilSøknadsperiode()
            dto.leggTilJournalposter(journalpostIder = journalpostIder)
            dto.pleietrengende?.leggTilPleietrengende()
            dto.bosteder?.mapTilBosteder()?.apply {
                pleipengerLivetsSluttfase.medBosteder(this)
            }
            dto.utenlandsopphold?.leggTilUtenlandsopphold()
            // Enn så lenge støtter vi kun å legge til opptjeningaktivitet eller ignorere
            // Sletting implementeres ved behov
            if (!dto.soeknadsperiode.isNullOrEmpty() || erOpptjeningSatt(dto, dto.opptjeningAktivitet)) {
                dto.opptjeningAktivitet?.mapOpptjeningAktivitet(feil)?.apply {
                    pleipengerLivetsSluttfase.medOpptjeningAktivitet(this)
                }
            } else {
                pleipengerLivetsSluttfase.ignorerOpplysningerOmOpptjening()
            }
            dto.arbeidstid?.mapTilArbeidstid(feil)?.apply {
                pleipengerLivetsSluttfase.medArbeidstid(this)
            }
            dto.trekkKravPerioder.leggTilTrekkKravPerioder()
            dto.leggTilBegrunnelseForInnsending()
            dto.uttak.leggTilUttak(søknadsperiode = dto.soeknadsperiode)

            // Fullfører søknad & validerer
            søknad.medYtelse(pleipengerLivetsSluttfase)
            feil.addAll(Validator.valider(søknad, perioderSomFinnesIK9.somK9Perioder()))
        }.onFailure { throwable ->
            logger.error("Uventet mappingfeil", throwable)
            feil.add(Feil("søknad", "uventetMappingfeil", throwable.message ?: "Uventet mappingfeil"))
        }
    }

    internal fun søknad() = søknad
    internal fun feil() = feil.toList()
    internal fun søknadOgFeil() = søknad() to feil()

    private fun String.leggTilSøknadId() {
        if (erSatt()) {
            søknad.medSøknadId(this)
        }
    }

    private fun String.leggTilVersjon() {
        søknad.medVersjon(this)
    }

    private fun PleiepengerLivetsSluttfaseSøknadDto.leggTilMottattDatoOgKlokkeslett() {
        if (mottattDato == null) {
            feil.add(Feil("søknad", "mottattDato", "Mottatt dato mangler"))
            return
        }
        if (klokkeslett == null) {
            feil.add(Feil("søknad", "klokkeslett", "Klokkeslett mangler"))
            return
        }

        søknad.medMottattDato(ZonedDateTime.of(mottattDato, klokkeslett, Oslo))
    }

    private fun PleietrengendeDto.leggTilPleietrengende() {
        val pleietrengende = Pleietrengende()
        when {
            norskIdent.erSatt() -> pleietrengende.medNorskIdentitetsnummer(NorskIdentitetsnummer.of(norskIdent))
            foedselsdato != null -> pleietrengende.medFødselsdato(foedselsdato)
        }
        pleipengerLivetsSluttfase.medPleietrengende(pleietrengende)
    }

    private fun String.leggTilSøker() {
        if (erSatt()) {
            søknad.medSøker(Søker(NorskIdentitetsnummer.of(this)))
        }
    }

    private fun List<UtenlandsoppholdDto>.leggTilUtenlandsopphold() {
        val k9Utenlandsopphold = mutableMapOf<Periode, Utenlandsopphold.UtenlandsoppholdPeriodeInfo>()
        filter { it.periode.erSatt() }.forEach { utenlandsopphold ->
            val k9Periode = utenlandsopphold.periode!!.somK9Periode()!!
            val k9Info = Utenlandsopphold.UtenlandsoppholdPeriodeInfo()
            if (utenlandsopphold.land.erSatt()) {
                k9Info.medLand(Landkode.of(utenlandsopphold.land))
            }
            if (utenlandsopphold.årsak.erSatt()) {
                mapEllerLeggTilFeil(feil, "ytelse.utenlandsopphold.${k9Periode.jsonPath()}.årsak") {
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
        if (begrunnelseForInnsending != null) {
            søknad.medBegrunnelseForInnsending(begrunnelseForInnsending)
        }
    }

    private fun PleiepengerLivetsSluttfaseSøknadDto.leggTilJournalposter(journalpostIder: Set<String>) {
        journalpostIder.forEach { journalpostId ->
            søknad.medJournalpost(
                Journalpost()
                    .medJournalpostId(journalpostId)
                    .medInformasjonSomIkkeKanPunsjes(harInfoSomIkkeKanPunsjes)
                    .medInneholderMedisinskeOpplysninger(harMedisinskeOpplysninger)
            )
        }
    }

    private fun Set<PeriodeDto>.leggTilTrekkKravPerioder() {
        pleipengerLivetsSluttfase.leggTilTrekkKravPerioder(this.somK9Perioder())
    }

    private fun List<PeriodeDto>.leggTilSøknadsperiode() {
        pleipengerLivetsSluttfase.medSøknadsperiode(this.somK9Perioder())
    }

    private fun List<PleiepengerLivetsSluttfaseSøknadDto.UttakDto>?.leggTilUttak(søknadsperiode: List<PeriodeDto>?) {
        val k9Uttak = mutableMapOf<Periode, Uttak.UttakPeriodeInfo>()

        this?.filter { it.periode.erSatt() }?.forEach { uttak ->
            val k9Periode = uttak.periode!!.somK9Periode()!!
            val k9Info = Uttak.UttakPeriodeInfo()
            mapEllerLeggTilFeil(
                feil = feil,
                felt = "ytelse.uttak.perioder.${k9Periode.jsonPath()}.timerPleieAvBarnetPerDag"
            ) { uttak.timerPleieAvPleietrengendePerDag?.somDuration() }?.also {
                k9Info.medTimerPleieAvBarnetPerDag(it)
            }
            k9Uttak[k9Periode] = k9Info
        }

        if (k9Uttak.isEmpty() && søknadsperiode != null) {
            søknadsperiode.forEach { periode ->
                periode.somK9Periode()?.let { k9Uttak[it] = DefaultUttak }
            }
        }

        if (k9Uttak.isNotEmpty()) {
            pleipengerLivetsSluttfase.medUttak(Uttak().medPerioder(k9Uttak))
        }
    }

    private fun erOpptjeningSatt(
        dto: PleiepengerLivetsSluttfaseSøknadDto,
        opptjeningAktivitet: ArbeidAktivitetDto?
    ) = dto.opptjeningAktivitet != null &&
        (
            !opptjeningAktivitet?.arbeidstaker.isNullOrEmpty() ||
                opptjeningAktivitet?.frilanser != null ||
                opptjeningAktivitet?.selvstendigNaeringsdrivende != null
            )

    internal companion object {
        private val logger = LoggerFactory.getLogger(MapPlsfTilK9Format::class.java)

        private val Validator = PleiepengerLivetsSluttfaseSøknadValidator()
        private const val Versjon = "1.0.0"
        private val DefaultUttak =
            Uttak.UttakPeriodeInfo().medTimerPleieAvBarnetPerDag(Duration.ofHours(7).plusMinutes(30))

        private fun Collection<PeriodeDto>.somK9Perioder() = mapNotNull { it.somK9Periode() }
        private fun String?.erSatt() = !isNullOrBlank()
    }
}
