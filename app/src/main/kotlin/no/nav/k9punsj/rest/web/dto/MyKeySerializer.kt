package no.nav.k9punsj.rest.web.dto

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.KeyDeserializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.k9punsj.objectMapper
import java.io.IOException

class MyKeySerializer: JsonSerializer<PeriodeDto>() {
    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(value: PeriodeDto?, gen: JsonGenerator?, serializers: SerializerProvider?) {
        gen?.let { jGen ->
            value?.let { movie ->
                jGen.writeFieldName(objectMapper().writeValueAsString(movie))
            } ?: jGen.writeNull()
        }
    }
}

class MyKeyDeserializer: KeyDeserializer() {
    @Throws(IOException::class, JsonProcessingException::class)
    override fun deserializeKey(key: String?, ctxt: DeserializationContext?): PeriodeDto? {
        return key?.let { objectMapper().readValue<PeriodeDto>(key) }
    }
}
