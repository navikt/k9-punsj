package no.nav.k9

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

fun objectMapper(): ObjectMapper {
    return jacksonObjectMapper()
            .dusseldorfConfigured()
            .configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE)
}

fun ObjectMapper.dusseldorfConfigured() : ObjectMapper {
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)
    configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)
    propertyNamingStrategy = PropertyNamingStrategy.LOWER_CAMEL_CASE
    registerModule(JavaTimeModule())
    return this
}