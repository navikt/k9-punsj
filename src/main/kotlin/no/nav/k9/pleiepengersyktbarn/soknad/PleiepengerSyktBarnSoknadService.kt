package no.nav.k9.pleiepengersyktbarn.soknad

import no.nav.k9.NorskIdent
import no.nav.k9.kafka.HendelseProducer
import no.nav.k9.mappe.Mappe
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


@Service
class PleiepengerSyktBarnSoknadService @Autowired constructor(
        var hendelseProducer: HendelseProducer
) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(PleiepengerSyktBarnSoknadService::class.java)
        const val PLEIEPENGER_SYKT_BARN_TOPIC = "punsjet-soknad-pleiepenger-barn"
    }

    internal suspend fun sendSøknad(
            norskIdent: NorskIdent,
            mappe: Mappe
    ) {
        val dummyJsonMessage = "{\"versjon\":\"1.0.0\",\"søknadId\":\"1\",\"mottattDato\":\"2019-10-20T07:15:36.124Z\",\"språk\":\"nb\",\"søker\":{\"norskIdentitetsnummer\":\"12345678901\"},\"perioder\":{\"2018-12-30/2019-10-20\":{}},\"barn\":{\"fødselsdato\":null,\"norskIdentitetsnummer\":\"12345678902\"},\"bosteder\":{\"perioder\":{\"2022-12-30/2023-10-10\":{\"land\":\"POL\"}}},\"utenlandsopphold\":{\"perioder\":{\"2018-12-30/2019-10-10\":{\"land\":\"SWE\",\"årsak\":\"barnetInnlagtIHelseinstitusjonForNorskOffentligRegning\"},\"2018-10-10/2018-10-30\":{\"land\":\"NOR\",\"årsak\":null},\"2021-10-10/2050-01-05\":{\"land\":\"DEN\",\"årsak\":\"barnetInnlagtIHelseinstitusjonDekketEtterAvtaleMedEtAnnetLandOmTrygd\"}}},\"beredskap\":{\"perioder\":{\"2018-10-10/2018-12-29\":{\"tilleggsinformasjon\":\"Noe tilleggsinformasjon. Lorem ipsum æÆøØåÅ.\"},\"2019-01-01/2019-01-30\":{\"tilleggsinformasjon\":\"Noe tilleggsinformasjon. Lorem ipsum æÆøØåÅ.\"}}},\"nattevåk\":{\"perioder\":{\"2018-10-10/2018-12-29\":{\"tilleggsinformasjon\":\"Noe tilleggsinformasjon. Lorem ipsum æÆøØåÅ.\"},\"2019-01-01/2019-01-30\":{\"tilleggsinformasjon\":\"Noe tilleggsinformasjon. Lorem ipsum æÆøØåÅ.\"}}},\"tilsynsordning\":{\"iTilsynsordning\":\"ja\",\"opphold\":{\"2019-01-01/2019-01-01\":{\"lengde\":\"PT7H30M\"},\"2020-01-02/2020-01-02\":{\"lengde\":\"PT7H25M\"},\"2020-01-03/2020-01-09\":{\"lengde\":\"PT168H\"}}},\"arbeid\":{\"arbeidstaker\":[{\"organisasjonsnummer\":\"999999999\",\"norskIdentitetsnummer\":null,\"perioder\":{\"2018-10-10/2018-12-29\":{\"skalJobbeProsent\":50.25}}},{\"organisasjonsnummer\":null,\"norskIdentitetsnummer\":\"29099012345\",\"perioder\":{\"2018-11-10/2018-12-29\":{\"skalJobbeProsent\":20}}}],\"selvstendigNæringsdrivende\":[{\"perioder\":{\"2018-11-11/2018-11-30\":{}}}],\"frilanser\":[{\"perioder\":{\"2019-10-10/2019-12-29\":{}}}]},\"lovbestemtFerie\":{\"perioder\":{\"2018-11-10/2018-12-29\":{}}}}\n"

        //TODO: Bytte ut dummy-json med reell punchet søknad
        hendelseProducer.sendToKafka(PLEIEPENGER_SYKT_BARN_TOPIC, dummyJsonMessage)
    }


}
