package no.nav.k9punsj.integrasjoner.k9sak

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpPost
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9.kodeverk.dokument.Brevkode
import no.nav.k9.sak.kontrakt.arbeidsforhold.InntektArbeidYtelseArbeidsforholdV2Dto
import no.nav.k9.sak.kontrakt.mottak.FinnEllerOpprettSak
import no.nav.k9.sak.typer.Periode
import no.nav.k9.søknad.Søknad
import no.nav.k9punsj.StandardProfil
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.felles.ZoneUtils.Oslo
import no.nav.k9punsj.felles.dto.ArbeidsgiverMedArbeidsforholdId
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.felles.dto.SaksnummerDto
import no.nav.k9punsj.felles.dto.SøknadEntitet
import no.nav.k9punsj.hentCallId
import no.nav.k9punsj.integrasjoner.k9sak.K9SakServiceImpl.Urls.finnFagsak
import no.nav.k9punsj.integrasjoner.k9sak.K9SakServiceImpl.Urls.hentIntektsmeldingerUrl
import no.nav.k9punsj.integrasjoner.k9sak.K9SakServiceImpl.Urls.hentPerioderUrl
import no.nav.k9punsj.integrasjoner.k9sak.K9SakServiceImpl.Urls.sendInnSøknadUrl
import no.nav.k9punsj.integrasjoner.k9sak.K9SakServiceImpl.Urls.sokFagsakerUrl
import no.nav.k9punsj.omsorgspengeraleneomsorg.tilOmsAOvisning
import no.nav.k9punsj.omsorgspengerkronisksyktbarn.tilOmsKSBvisning
import no.nav.k9punsj.omsorgspengermidlertidigalene.tilOmsMAvisning
import no.nav.k9punsj.omsorgspengerutbetaling.tilOmsUtvisning
import no.nav.k9punsj.opplaeringspenger.tilOlpvisning
import no.nav.k9punsj.pleiepengerlivetssluttfase.tilPlsvisning
import no.nav.k9punsj.pleiepengersyktbarn.tilPsbvisning
import no.nav.k9punsj.utils.objectMapper
import org.intellij.lang.annotations.Language
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import java.net.URI
import java.time.LocalDate
import java.util.Base64
import java.util.UUID
import kotlin.coroutines.coroutineContext

@Configuration
@StandardProfil
class K9SakServiceImpl(
    @Value("\${no.nav.k9sak.base_url}") private val baseUrl: URI,
    @Value("\${no.nav.k9sak.scope}") private val k9sakScope: Set<String>,
    @Qualifier("sts") private val accessTokenClient: AccessTokenClient,
    private val personService: PersonService
) : K9SakService {

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)
    private val log = LoggerFactory.getLogger("K9SakService")

    internal object Urls {
        internal const val hentPerioderUrl = "/behandling/soknad/perioder"
        internal const val hentIntektsmeldingerUrl = "/behandling/iay/im-arbeidsforhold-v2"
        internal const val sokFagsakerUrl = "/fagsak/sok"
        internal const val sendInnSøknadUrl = "/fordel/journalposter"
        internal const val sokFagsaker = "/fagsak/sok"
        internal const val finnFagsak = "/fordel/fagsak/sok"
    }

    override suspend fun hentPerioderSomFinnesIK9(
        søker: String,
        barn: String?,
        fagsakYtelseType: no.nav.k9punsj.felles.FagsakYtelseType,
    ): Pair<List<PeriodeDto>?, String?> {
        val matchDto = MatchDto(
            ytelseType = FagsakYtelseType.fraKode(fagsakYtelseType.kode),
            bruker = søker,
            pleietrengende = barn
        )

        val body = kotlin.runCatching { objectMapper().writeValueAsString(matchDto) }.getOrNull()
            ?: return Pair(null, "Feilet serialisering")

        val (json, feil) = httpPost(body, hentPerioderUrl)
        return try {
            if (json == null) {
                return Pair(null, feil!!)
            }
            val resultat = objectMapper().readValue<List<Periode>>(json)
            val liste = resultat
                .map { periode -> PeriodeDto(periode.fom, periode.tom) }.toList()
            Pair(liste, null)
        } catch (e: Exception) {
            Pair(null, "Feilet deserialisering $e")
        }
    }

    /*
     * 1. Slår opp saksnummer basert på ytelsetype, periode & søkers aktørId.
     * 2. Henter perioder for saksnummer.
     */
    override suspend fun hentPerioderSomFinnesIK9ForPeriode(
        søker: String,
        barn: String?,
        fagsakYtelseType: no.nav.k9punsj.felles.FagsakYtelseType,
        periode: PeriodeDto
    ): Pair<List<PeriodeDto>?, String?> {
        val søkerAktørId = personService.finnAktørId(søker)
        val barnAktørId = barn?.let { personService.finnAktørId(barn) }
        val finnFagsakDto = FinnFagsakDto(
            ytelseType = FagsakYtelseType.fraKode(fagsakYtelseType.kode),
            aktørId = søkerAktørId,
            pleietrengendeAktørId = barnAktørId,
            periode = periode
        )

        val saksnummerBody = kotlin.runCatching { objectMapper().writeValueAsString(finnFagsakDto) }.getOrNull()
            ?: return Pair(null, "Feilet serialisering")

        val (saksnummerJson, saksnummerFeil) = httpPost(saksnummerBody, finnFagsak)
        saksnummerFeil?.let { Pair(null, saksnummerFeil) }

        val saksnummer = saksnummerJson?.let { objectMapper().readValue<SaksnummerDto>(it) }
            ?: return Pair(null, "Fant ikke saksnummer")

        val (json, feil) = httpPost(
            saksnummerJson,
            "/behandling/soknad/perioder/saksnummer?saksnummer=${saksnummer.saksnummer}"
        )
        return try {
            if (json == null) {
                return Pair(null, feil!!)
            }
            val resultat = objectMapper().readValue<List<Periode>>(json)
            val liste = resultat
                .map { periode -> PeriodeDto(periode.fom, periode.tom) }.toList()
            Pair(liste, null)
        } catch (e: Exception) {
            Pair(null, "Feilet deserialisering $e")
        }
    }

    override suspend fun hentArbeidsforholdIdFraInntektsmeldinger(
        søker: String,
        fagsakYtelseType: no.nav.k9punsj.felles.FagsakYtelseType,
        periodeDto: PeriodeDto,
    ): Pair<List<ArbeidsgiverMedArbeidsforholdId>?, String?> {
        val matchDto = MatchArbeidsforholdDto(
            ytelseType = FagsakYtelseType.fraKode(fagsakYtelseType.kode),
            bruker = søker,
            periode = periodeDto
        )

        val body = kotlin.runCatching { objectMapper().writeValueAsString(matchDto) }.getOrNull()
            ?: return Pair(null, "Feilet serialisering")

        val (json, feil) = httpPost(body, hentIntektsmeldingerUrl)

        return try {
            if (json == null) {
                return Pair(null, feil)
            }
            val dataSett = objectMapper().readValue<Set<InntektArbeidYtelseArbeidsforholdV2Dto>>(json)
            val map = dataSett.groupBy { it.arbeidsgiver }.map { entry ->
                ArbeidsgiverMedArbeidsforholdId(
                    orgNummerEllerAktørID = entry.key.identifikator,
                    arbeidsforholdId = entry.value.map { it.arbeidsforhold.eksternArbeidsforholdId }
                )
            }
            Pair(map, null)
        } catch (e: Exception) {
            Pair(null, "Feilet deserialisering $e")
        }
    }

    override suspend fun hentFagsaker(søker: String): Pair<Set<Fagsak>?, String?> {
        @Language("json")
        val body = """
            {
              "searchString": "$søker"
            }
        """.trimIndent()

        val (json, feil) = httpPost(body, sokFagsakerUrl)

        return if (!json.isNullOrEmpty()) {
            Pair(json.fagsaker(), null)
        } else {
            Pair(null, feil)
        }
    }

    override suspend fun hentEllerOpprettSaksnummer(
        søknadEntitet: SøknadEntitet,
        fagsakYtelseType: no.nav.k9punsj.felles.FagsakYtelseType
    ): Pair<String?, String?> {
        val k9SaksnummerGrunnlag = søknadEntitet.tilK9saksnummerGrunnlag(fagsakYtelseType)

        val søkerAktørId = personService.finnEllerOpprettPersonVedNorskIdent(k9SaksnummerGrunnlag.søker).aktørId
        val pleietrengendeAktørId =
            if (!k9SaksnummerGrunnlag.pleietrengende.isNullOrEmpty() && k9SaksnummerGrunnlag.pleietrengende != "null") {
                personService.finnEllerOpprettPersonVedNorskIdent(k9SaksnummerGrunnlag.pleietrengende).aktørId
            } else null
        val annenpartAktørId =
            if (!k9SaksnummerGrunnlag.annenPart.isNullOrEmpty() && k9SaksnummerGrunnlag.annenPart != "null") {
                personService.finnEllerOpprettPersonVedNorskIdent(k9SaksnummerGrunnlag.annenPart).aktørId
            } else null
        val periode = Periode(
            k9SaksnummerGrunnlag.periode?.fom ?: LocalDate.now().withMonth(1).withDayOfMonth(1),
            k9SaksnummerGrunnlag.periode?.tom ?: LocalDate.now().withMonth(12).withDayOfMonth(31)
        )

        val payloadMedAktørId = FinnEllerOpprettSak(
            FagsakYtelseType.fraKode(k9SaksnummerGrunnlag.søknadstype.kode).kode,
            søkerAktørId,
            pleietrengendeAktørId,
            annenpartAktørId,
            periode,
        )

        val body = kotlin.runCatching { objectMapper().writeValueAsString(payloadMedAktørId) }.getOrNull()
            ?: return Pair(null, "Feilet serialisering")
        val response = httpPost(body, "/fordel/fagsak/opprett")

        return try {
            val saksnummer = JSONObject(response.first).getString("saksnummer").toString()
            Pair(saksnummer, null)
        } catch (e: Exception) {
            Pair(null, "Feilet deserialisering")
        }

    }

    override suspend fun sendInnSoeknad(
        soknad: Søknad,
        journalpostId: String,
        fagsakYtelseType: no.nav.k9punsj.felles.FagsakYtelseType,
        saksnummer: String,
        brevkode: Brevkode
    ) {
        val forsendelseMottattTidspunkt = soknad.mottattDato.withZoneSameInstant(Oslo).toLocalDateTime()
        val søknadJson = objectMapper().writeValueAsString(soknad)
        val callId = hentCallId()

        // https://github.com/navikt/k9-sak/blob/3.1.30/kontrakt/src/main/java/no/nav/k9/sak/kontrakt/mottak/JournalpostMottakDto.java#L31
        @Language("JSON")
        val body = """
            [{
                "saksnummer": "$saksnummer",
                "journalpostId": "$journalpostId",
                "ytelseType": {
                    "kode": "${fagsakYtelseType.kode}",
                    "kodeverk": "FAGSAK_YTELSE"
                },
                "kanalReferanse": "$callId",
                "type": "${brevkode.kode}",
                "forsendelseMottattTidspunkt": "$forsendelseMottattTidspunkt",
                "forsendelseMottatt": "${forsendelseMottattTidspunkt.toLocalDate()}",
                "payload": "${Base64.getUrlEncoder().encodeToString(søknadJson.toString().toByteArray())}"
            }]
        """.trimIndent()

        val (_, feil) = httpPost(body, sendInnSøknadUrl)
        require(feil.isNullOrEmpty()) {
            log.error("Feil ved sending av søknad til k9-sak: $feil")
            throw IllegalStateException("Feil ved sending av søknad til k9-sak: $feil")
        }
    }

    private suspend fun httpPost(body: String, url: String): Pair<String?, String?> {
        val (request, _, result) = "$baseUrl$url"
            .httpPost()
            .body(body)
            .header(
                HttpHeaders.ACCEPT to "application/json",
                HttpHeaders.AUTHORIZATION to cachedAccessTokenClient.getAccessToken(emptySet()).asAuthoriationHeader(),
                HttpHeaders.CONTENT_TYPE to "application/json",
                "callId" to hentCallId()
            ).awaitStringResponseResult()

        return result.fold(
            { success ->
                Pair(success, null)
            },
            { error ->
                log.error(
                    "Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'"
                )
                log.error(error.toString())
                Pair(null, "Feil ved kall til k9-sak")
            }
        )
    }

    private fun SøknadEntitet.tilK9saksnummerGrunnlag(
        fagsakYtelseType: no.nav.k9punsj.felles.FagsakYtelseType
    ): HentK9SaksnummerGrunnlag {
        // Kontrakt  fra k9-sak FagsakYtelseType
        // https://github.com/navikt/k9-sak/blob/83b654701d242629a20eabbab642c399c6c27182/kodeverk/src/main/java/no/nav/k9/kodeverk/behandling/FagsakYtelseType.java#L57
        return when (fagsakYtelseType) {
            no.nav.k9punsj.felles.FagsakYtelseType.PLEIEPENGER_SYKT_BARN -> {
                val psbVisning = this.tilPsbvisning()
                HentK9SaksnummerGrunnlag(
                    søknadstype = fagsakYtelseType,
                    søker = psbVisning.soekerId!!,
                    pleietrengende = psbVisning.barn?.norskIdent,
                    annenPart = null,
                    periode = psbVisning.soeknadsperiode?.firstOrNull()
                )
            }

            no.nav.k9punsj.felles.FagsakYtelseType.OMSORGSPENGER -> {
                val omsVisning = this.tilOmsUtvisning()
                HentK9SaksnummerGrunnlag(
                    søknadstype = fagsakYtelseType,
                    søker = omsVisning.soekerId!!,
                    pleietrengende = null,
                    annenPart = null,
                    periode = omsVisning.periodeForHeleAretMedFravaer()
                )
            }

            no.nav.k9punsj.felles.FagsakYtelseType.OMSORGSPENGER_UTBETALING -> {
                val omsUtvisning = this.tilOmsUtvisning()
                HentK9SaksnummerGrunnlag(
                    søknadstype = fagsakYtelseType,
                    søker = omsUtvisning.soekerId!!,
                    pleietrengende = null,
                    annenPart = null,
                    periode = omsUtvisning.periodeForHeleAretMedFravaer()
                )
            }

            no.nav.k9punsj.felles.FagsakYtelseType.OMSORGSPENGER_ALENE_OMSORGEN -> {
                val omsAoVisning = this.tilOmsAOvisning()
                HentK9SaksnummerGrunnlag(
                    søknadstype = fagsakYtelseType,
                    søker = omsAoVisning.soekerId!!,
                    pleietrengende = omsAoVisning.barn!!.norskIdent,
                    annenPart = null,
                    periode = omsAoVisning.periode
                )
            }

            no.nav.k9punsj.felles.FagsakYtelseType.OMSORGSPENGER_MIDLERTIDIG_ALENE -> {
                val omsMaVisning = this.tilOmsMAvisning()
                HentK9SaksnummerGrunnlag(
                    søknadstype = fagsakYtelseType,
                    søker = omsMaVisning.soekerId!!,
                    pleietrengende = null,
                    annenPart = omsMaVisning.annenForelder!!.norskIdent,
                    periode = omsMaVisning.annenForelder.periode
                )
            }

            no.nav.k9punsj.felles.FagsakYtelseType.OMSORGSPENGER_KRONISK_SYKT_BARN -> {
                val omsKsVisning = this.tilOmsKSBvisning()
                HentK9SaksnummerGrunnlag(
                    søknadstype = fagsakYtelseType,
                    søker = omsKsVisning.soekerId!!,
                    pleietrengende = omsKsVisning.barn!!.norskIdent,
                    annenPart = null,
                    periode = omsKsVisning.mottattDato?.periodeDtoForHeleAret()
                )
            }

            no.nav.k9punsj.felles.FagsakYtelseType.PLEIEPENGER_LIVETS_SLUTTFASE -> {
                val tilPlsvisning = this.tilPlsvisning()
                HentK9SaksnummerGrunnlag(
                    søknadstype = fagsakYtelseType,
                    søker = tilPlsvisning.soekerId!!,
                    pleietrengende = tilPlsvisning.pleietrengende!!.norskIdent,
                    annenPart = null,
                    periode = tilPlsvisning.soeknadsperiode?.firstOrNull()
                )
            }

            no.nav.k9punsj.felles.FagsakYtelseType.OPPLÆRINGSPENGER -> {
                val tilOlpvisning = this.tilOlpvisning()
                HentK9SaksnummerGrunnlag(
                    søknadstype = fagsakYtelseType,
                    søker = tilOlpvisning.soekerId!!,
                    pleietrengende = tilOlpvisning.barn!!.norskIdent,
                    annenPart = null,
                    periode = tilOlpvisning.soeknadsperiode?.firstOrNull()
                )
            }

            no.nav.k9punsj.felles.FagsakYtelseType.UKJENT -> throw IllegalStateException("Ustøttet fagsakytelsetype")
            no.nav.k9punsj.felles.FagsakYtelseType.UDEFINERT -> throw IllegalStateException("Ustøttet fagsakytelsetype")
        }
    }

    private fun LocalDate.periodeDtoForHeleAret(): PeriodeDto {
        return PeriodeDto(
            fom = this.withMonth(1).withDayOfMonth(1),
            tom = this.withMonth(12).withDayOfMonth(31)
        )
    }

    internal companion object {
        private suspend fun hentCallId() = try {
            coroutineContext.hentCallId()
        } catch (e: Exception) {
            UUID.randomUUID().toString()
        }

        private fun String.fagsaker() = JSONArray(this)
            .asSequence()
            .map { it as JSONObject }
            .map {
                val saksnummer = it.getString("saksnummer")
                val sakstypeKode = it.getJSONObject("sakstype").getString("kode")
                val pleietrengende = it.getStringOrNull("pleietrengendeAktørId")
                val fagsakYtelseType = FagsakYtelseType.fraKode(sakstypeKode)
                val gyldigPeriode: JSONObject? = it.optJSONObject("gyldigPeriode")
                val periodeDto: PeriodeDto? = gyldigPeriode?.somPeriodeDto()
                Fagsak(
                    saksnummer = saksnummer,
                    sakstype = fagsakYtelseType,
                    pleietrengendeAktorId = pleietrengende,
                    gyldigPeriode = periodeDto
                )
            }.toSet()

        private fun JSONObject.getStringOrNull(key: String) = if (this.has(key)) this.getString(key) else null
        private fun JSONObject.somPeriodeDto() = PeriodeDto(
            fom = LocalDate.parse(getString("fom")),
            tom = LocalDate.parse(getString("tom"))
        )

        private data class MatchDto(
            val ytelseType: FagsakYtelseType,
            val bruker: String,
            val pleietrengende: String? = null,
        )

        data class HentSaksnummerForPeriodeDto(
            val ytelseType: FagsakYtelseType,
            val bruker: String,
            val pleietrengende: List<String>? = null,
            val periode: PeriodeDto?
        )

        data class FinnFagsakDto(
            val ytelseType: FagsakYtelseType,
            val aktørId: String,
            val pleietrengendeAktørId: String? = null,
            val periode: PeriodeDto
        )

        data class MatchArbeidsforholdDto(
            val ytelseType: FagsakYtelseType,
            val bruker: String,
            val periode: PeriodeDto,
        )
    }
}
