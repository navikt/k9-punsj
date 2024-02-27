package no.nav.k9punsj.utils

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.k9.kodeverk.api.Kodeverdi

internal fun objectMapper(kodeverdiSomString: Boolean = false): ObjectMapper {
    val objectMapper = jacksonObjectMapper()
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .enable(SerializationFeature.INDENT_OUTPUT)
        .setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
        .registerModule(JavaTimeModule())

    if (kodeverdiSomString){
        //midlertidig, kan tas bort når kontrakt mot k9-sak er oppdatert til @JsonValue for kodeverk
        val m = SimpleModule()
        m.addSerializer(TempKodeverkSerializer())
        return objectMapper.registerModule(m)
    }

    return objectMapper
}

class TempKodeverkSerializer() : StdSerializer<Kodeverdi>(Kodeverdi::class.java) {

    override fun serialize(value: Kodeverdi?, gen: JsonGenerator?, provider: SerializerProvider?) {
        gen!!.writeString(value!!.kode);
    }

}