package no.nav.k9punsj.rest.eksternt.k9sak

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.k9punsj.db.datamodell.FagsakYtelseType
import no.nav.k9punsj.db.datamodell.NorskIdent
import no.nav.k9punsj.db.datamodell.Periode
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.web.SøknadJson
import no.nav.k9punsj.rest.web.dto.SaksnummerDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.ReactiveHealthIndicator
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import reactor.core.publisher.Mono
import java.net.URI

@Configuration
@Profile("!test")
class K9SakServiceImpl(
    @Value("\${no.nav.pdl.base_url}") baseUrl: URI,
) : ReactiveHealthIndicator, K9SakService {

//    @Qualifier("sts") private val accessTokenClient: AccessTokenClient
//    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)


    override fun health(): Mono<Health> {
        TODO("Not yet implemented")
    }

    override suspend fun hentSisteMottattePsbSøknad(norskIdent: NorskIdent, periode: Periode): SøknadJson? {
        val json = lesFraFil()
        return objectMapper().readValue<MutableMap<String, Any?>>(json)
    }

    override suspend fun opprettEllerHentFagsakNummer(): SaksnummerDto {
        TODO("Not yet implemented")
    }

    override suspend fun hentPerioderSomFinnesIK9(
        søker: NorskIdent,
        barn: NorskIdent,
        fagsakYtelseType: FagsakYtelseType,
    ) {
        TODO("Not yet implemented")
    }

    private fun lesFraFil(): String {
        return "{\n" +
                "  \"søknadId\": \"1\",\n" +
                "  \"versjon\": \"2.0.0\",\n" +
                "  \"mottattDato\": \"2020-10-12T12:53:21.046Z\",\n" +
                "  \"søker\": {\n" +
                "    \"norskIdentitetsnummer\": \"11111111111\"\n" +
                "  },\n" +
                "  \"ytelse\": {\n" +
                "    \"type\" : \"PLEIEPENGER_SYKT_BARN\",\n" +
                "    \"søknadsperiode\" : \"2018-12-30/2019-10-20\",\n" +
                "    \"barn\" : {\n" +
                "      \"norskIdentitetsnummer\" : \"11111111111\",\n" +
                "      \"fødselsdato\" : null\n" +
                "    },\n" +
                "    \"arbeidAktivitet\" : {\n" +
                "      \"selvstendigNæringsdrivende\" : [ {\n" +
                "        \"perioder\" : {\n" +
                "          \"2018-11-11/2018-11-30\" : {\n" +
                "            \"virksomhetstyper\" : [ \"FISKE\" ]\n" +
                "          }\n" +
                "        },\n" +
                "        \"virksomhetNavn\" : \"Test\"\n" +
                "      } ],\n" +
                "      \"frilanser\" : {\n" +
                "        \"startdato\" : \"2019-10-10\",\n" +
                "        \"jobberFortsattSomFrilans\" : true\n" +
                "      }\n" +
                "    },\n" +
                "    \"beredskap\" : {\n" +
                "      \"perioder\" : {\n" +
                "        \"2019-02-21/2019-05-21\" : {\n" +
                "          \"tilleggsinformasjon\" : \"Noe tilleggsinformasjon. Lorem ipsum æÆøØåÅ.\"\n" +
                "        },\n" +
                "        \"2018-12-30/2019-02-20\" : {\n" +
                "          \"tilleggsinformasjon\" : \"Noe tilleggsinformasjon. Lorem ipsum æÆøØåÅ.\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"nattevåk\" : {\n" +
                "      \"perioder\" : {\n" +
                "        \"2019-02-21/2019-05-21\" : {\n" +
                "          \"tilleggsinformasjon\" : \"Noe tilleggsinformasjon. Lorem ipsum æÆøØåÅ.\"\n" +
                "        },\n" +
                "        \"2018-12-30/2019-02-20\" : {\n" +
                "          \"tilleggsinformasjon\" : \"Noe tilleggsinformasjon. Lorem ipsum æÆøØåÅ.\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"tilsynsordning\" : {\n" +
                "      \"perioder\" : {\n" +
                "        \"2019-01-01/2019-01-01\" : {\n" +
                "          \"etablertTilsynTimerPerDag\" : \"PT7H30M\"\n" +
                "        },\n" +
                "        \"2019-01-02/2019-01-02\" : {\n" +
                "          \"etablertTilsynTimerPerDag\" : \"PT7H30M\"\n" +
                "        },\n" +
                "        \"2019-01-03/2019-01-09\" : {\n" +
                "          \"etablertTilsynTimerPerDag\" : \"PT7H30M\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"arbeidstid\" : {\n" +
                "      \"arbeidstakerList\" : [ {\n" +
                "        \"norskIdentitetsnummer\" : null,\n" +
                "        \"organisasjonsnummer\" : \"999999999\",\n" +
                "        \"arbeidstidInfo\" : {\n" +
                "          \"jobberNormaltTimerPerDag\" : \"PT7H30M\",\n" +
                "          \"perioder\" : {\n" +
                "            \"2018-12-30/2019-10-20\" : {\n" +
                "              \"faktiskArbeidTimerPerDag\" : \"PT7H30M\"\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      } ],\n" +
                "      \"frilanserArbeidstidInfo\" : null,\n" +
                "      \"selvstendigNæringsdrivendeArbeidstidInfo\" : null\n" +
                "    },\n" +
                "    \"uttak\" : {\n" +
                "      \"perioder\" : {\n" +
                "        \"2018-12-30/2019-10-20\" : {\n" +
                "          \"timerPleieAvBarnetPerDag\" : \"PT7H30M\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"omsorg\" : {\n" +
                "      \"relasjonTilBarnet\" : \"MORA\",\n" +
                "      \"samtykketOmsorgForBarnet\" : true,\n" +
                "      \"beskrivelseAvOmsorgsrollen\" : \"Noe tilleggsinformasjon. Lorem ipsum æÆøØåÅ.\"\n" +
                "    },\n" +
                "    \"lovbestemtFerie\" : {\n" +
                "      \"perioder\" : [ \"2019-02-21/2019-05-21\" ]\n" +
                "    },\n" +
                "    \"bosteder\" : {\n" +
                "      \"perioder\" : {\n" +
                "        \"2018-12-30/2019-10-20\" : {\n" +
                "          \"land\" : \"DNK\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"utenlandsopphold\" : {\n" +
                "      \"perioder\" : {\n" +
                "        \"2018-12-30/2019-10-20\" : {\n" +
                "          \"land\" : \"DNK\",\n" +
                "          \"årsak\" : \"barnetInnlagtIHelseinstitusjonForNorskOffentligRegning\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n"
    }
}
