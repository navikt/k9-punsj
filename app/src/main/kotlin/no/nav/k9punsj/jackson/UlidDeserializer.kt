package no.nav.k9punsj.jackson

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import de.huxhorn.sulky.ulid.ULID
import java.io.IOException
import kotlin.jvm.Throws

class UlidDeserializer : JsonDeserializer<ULID.Value>() {
    @Throws(IOException::class, JsonProcessingException::class)
    override fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): ULID.Value {
        return ULID.parseULID(jsonParser.valueAsString)
    }
}

