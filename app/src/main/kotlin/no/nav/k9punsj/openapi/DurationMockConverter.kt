package no.nav.k9punsj.openapi

import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.converter.ModelConverter
import io.swagger.v3.core.converter.ModelConverterContext
import io.swagger.v3.core.util.Json
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import java.time.Duration

/*
Denne konvertereren brukes kun fordi OpenAPI
ikke har implementert java.time.Duration - noe som førte til at api-dokumentasjon ble ubrukelig.
https://github.com/swagger-api/swagger-core/issues/1445
https://github.com/swagger-api/swagger-core/issues/2784
 */
class DurationMockConverter : ModelConverter {
    override fun resolve(
            type: AnnotatedType,
            context: ModelConverterContext,
            chain: Iterator<ModelConverter>): Schema<*>? {

        if (type.isSchemaProperty) {
            val _type = Json.mapper().constructType(type.type)
            if (_type != null) {
                val cls = _type.rawClass
                if (Duration::class.java.isAssignableFrom(cls)) {
                    return ObjectSchema().example("PT7H25M")
                }
            }
        }
        return if (chain.hasNext()) {
            chain.next().resolve(type, context, chain)
        } else {
            null
        }
    }
}
