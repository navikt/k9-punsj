package no.nav.k9.pleiepengersyktbarn.soknad

import no.nav.k9.*
import no.nav.k9.mappe.Mappe
import org.apache.kafka.clients.producer.KafkaProducer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SslConfigs
import org.springframework.beans.factory.annotation.Value
import java.util.*


@Service
class KafkaPropertiesHelper {
    @Value("\${kafka.bootstrap.server}")
    val server= ""
    @Value("\${kafka.ack}")
    val ack="1"
    @Value("\${kafka.retries}")
    val retries="0"
    @Value("\${kafka.batch.size}")
    val size="33554432"
    @Value("\${kafka.linger.ms}")
    val ms="1"
    @Value("\${kafka.buffer.memory}")
    val memory="33554432"
    @Value("\${kafka.key.serializer}")
    val key="org.apache.kafka.common.serialization.StringSerializer"
    @Value("\${kafka.value.serializer}")
    val value="org.apache.kafka.common.serialization.StringSerializer"
    @Value("\${kafka.security.protocol}")
    val protocol="SASL_SSL"
    @Value("\${kafka.sasl.mechanism}")
    val mechanism="SASL_SSL"
    @Value("\${kafka.sslconfig.truststore}")
    val truststore="/Users/jankaspermartinsen/.modig/truststore.jks"
    @Value("\${kafka.sslconfig.password}")
    val password="changeit"
}

@Service
internal class PleiepengerSyktBarnSoknadService {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(PleiepengerSyktBarnSoknadService::class.java)
    }

    @Autowired
    lateinit var kafkapropertieshelper : KafkaPropertiesHelper

    internal suspend fun sendSøknad(
            norskIdent: NorskIdent,
            mappe: Mappe
    ) {

        sendKafkaMessage("privat-omsorgspengesoknad-journalfort")

        // TODO: Legge på en kafka-topic k9-fordel håndterer.
        logger.info("sendSøknad")
        logger.info("NorskIdent=$norskIdent")
        logger.info("Mappe=$mappe")
    }

    fun sendKafkaMessage(topic:String){

        val props = Properties()
        props["bootstrap.servers"] = kafkapropertieshelper.server
        props["acks"] = kafkapropertieshelper.ack
        props["retries"] = kafkapropertieshelper.retries
        props["batch.size"] = kafkapropertieshelper.size
        props["linger.ms"] = kafkapropertieshelper.ms
        props["buffer.memory"] = kafkapropertieshelper.memory
        props["key.serializer"] = kafkapropertieshelper.key
        props["value.serializer"] = kafkapropertieshelper.value
        props["security.protocol"] = kafkapropertieshelper.protocol
        props["sasl.mechanism"] = kafkapropertieshelper.mechanism
        props["sasl.jaas.config"] = "org.apache.kafka.common.security.plain.PlainLoginModule required\n" +
                "username=\"vtp\"\n" +
                "password=\"vtp\";"
        props[SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG] = kafkapropertieshelper.truststore
        props[SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG] = kafkapropertieshelper.password

        val dummyJsonMessage = "{\n" +
                "  \"versjon\" : \"1.0.0\",\n" +
                "  \"søknadId\": \"1\",\n" +
                "  \"mottattDato\" : \"2019-10-20T07:15:36.124Z\",\n" +
                "  \"språk\": \"nb\",\n" +
                "  \"søker\" : {\n" +
                "    \"norskIdentitetsnummer\" : \"12345678901\"\n" +
                "  },\n" +
                "  \"perioder\" : {\n" +
                "    \"2018-12-30/2019-10-20\": {}\n" +
                "  },\n" +
                "  \"barn\" : {\n" +
                "    \"fødselsdato\": null,\n" +
                "    \"norskIdentitetsnummer\" : \"12345678902\"\n" +
                "  },\n" +
                "  \"bosteder\": {\n" +
                "    \"perioder\": {\n" +
                "      \"2022-12-30/2023-10-10\" : {\n" +
                "        \"land\": \"POL\"\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"utenlandsopphold\": {\n" +
                "    \"perioder\": {\n" +
                "      \"2018-12-30/2019-10-10\" : {\n" +
                "        \"land\": \"SWE\",\n" +
                "        \"årsak\": \"barnetInnlagtIHelseinstitusjonForNorskOffentligRegning\"\n" +
                "      },\n" +
                "      \"2018-10-10/2018-10-30\" : {\n" +
                "        \"land\": \"NOR\",\n" +
                "        \"årsak\": null\n" +
                "      },\n" +
                "      \"2021-10-10/2050-01-05\" : {\n" +
                "        \"land\": \"DEN\",\n" +
                "        \"årsak\": \"barnetInnlagtIHelseinstitusjonDekketEtterAvtaleMedEtAnnetLandOmTrygd\"\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"beredskap\": {\n" +
                "    \"perioder\": {\n" +
                "      \"2018-10-10/2018-12-29\": {\n" +
                "        \"tilleggsinformasjon\": \"Noe tilleggsinformasjon. Lorem ipsum æÆøØåÅ.\"\n" +
                "      },\n" +
                "      \"2019-01-01/2019-01-30\": {\n" +
                "        \"tilleggsinformasjon\": \"Noe tilleggsinformasjon. Lorem ipsum æÆøØåÅ.\"\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"nattevåk\": {\n" +
                "    \"perioder\": {\n" +
                "      \"2018-10-10/2018-12-29\" : {\n" +
                "        \"tilleggsinformasjon\": \"Noe tilleggsinformasjon. Lorem ipsum æÆøØåÅ.\"\n" +
                "      },\n" +
                "      \"2019-01-01/2019-01-30\": {\n" +
                "        \"tilleggsinformasjon\": \"Noe tilleggsinformasjon. Lorem ipsum æÆøØåÅ.\"\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"tilsynsordning\": {\n" +
                "    \"iTilsynsordning\": \"ja\",\n" +
                "    \"opphold\": {\n" +
                "      \"2019-01-01/2019-01-01\": {\n" +
                "        \"lengde\": \"PT7H30M\"\n" +
                "      },\n" +
                "      \"2020-01-02/2020-01-02\": {\n" +
                "        \"lengde\": \"PT7H25M\"\n" +
                "      },\n" +
                "      \"2020-01-03/2020-01-09\": {\n" +
                "        \"lengde\": \"PT168H\"\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"arbeid\": {\n" +
                "    \"arbeidstaker\": [{\n" +
                "      \"organisasjonsnummer\": \"999999999\",\n" +
                "      \"norskIdentitetsnummer\": null,\n" +
                "      \"perioder\" : {\n" +
                "        \"2018-10-10/2018-12-29\": {\n" +
                "          \"skalJobbeProsent\": 50.25\n" +
                "        }\n" +
                "      }\n" +
                "    },{\n" +
                "      \"organisasjonsnummer\": null,\n" +
                "      \"norskIdentitetsnummer\": \"29099012345\",\n" +
                "      \"perioder\" : {\n" +
                "        \"2018-11-10/2018-12-29\": {\n" +
                "          \"skalJobbeProsent\": 20.00\n" +
                "        }\n" +
                "      }\n" +
                "    }],\n" +
                "    \"selvstendigNæringsdrivende\": [{\n" +
                "      \"perioder\" : {\n" +
                "        \"2018-11-11/2018-11-30\": {}\n" +
                "      }\n" +
                "    }],\n" +
                "    \"frilanser\": [{\n" +
                "      \"perioder\" : {\n" +
                "        \"2019-10-10/2019-12-29\": {}\n" +
                "      }\n" +
                "    }]\n" +
                "  },\n" +
                "  \"lovbestemtFerie\":  {\n" +
                "    \"perioder\": {\n" +
                "      \"2018-11-10/2018-12-29\": {}\n" +
                "    }\n" +
                "  }\n" +
                "}"

        val producer = KafkaProducer<String, String>(props)
        try {
            producer.send(ProducerRecord(topic, dummyJsonMessage)).get()
            logger.info("sendte en message på en kafka topic")
        }catch (e:Exception){
            logger.warn("feil")

        }finally {
            producer.flush()
            producer.close()
        }
    }
}
