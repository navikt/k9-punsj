package no.nav.k9

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import javax.validation.ConstraintViolation

internal val objectMapper = jacksonObjectMapper()

typealias JournalpostId = String

typealias Innhold = MutableMap<String, Any?>

data class Innsending(val innhold: MutableList<Innhold>)

internal fun Innsending.oppdater(nyInnsending: Innsending) : Innsending {
    innhold.forEachIndexed { index, eksisterendeInnhold ->
        val før = objectMapper.valueToTree<ObjectNode>(eksisterendeInnhold)
        val nytt = objectMapper.valueToTree<ObjectNode>(nyInnsending.innhold[index])
        val map = objectMapper.convertValue<Map<String, Any?>>(merge(før, nytt))
        eksisterendeInnhold.clear()
        eksisterendeInnhold.putAll(map)
    }
    return this
}

data class MellomlagringsResultat(
        val innhold: List<Innhold>,
        private val violations: Set<ConstraintViolation<*>>
) {
    val feil = violations.map { Feil(
            attributt = it.propertyPath.toString(),
            ugyldigVerdi = it.invalidValue,
            melding = it.message
    )}
}

data class Feil(
        val attributt: String,
        @JsonProperty(value = "ugyldig_verdi")
        val ugyldigVerdi: Any?,
        val melding: String
)

private fun merge(mainNode: JsonNode, updateNode: JsonNode): JsonNode {
    updateNode.fieldNames().forEach { fieldName ->
        val jsonNode = mainNode.get(fieldName)
        if (jsonNode != null && jsonNode.isObject) {
            merge(jsonNode, updateNode.get(fieldName))
        } else {
            if (mainNode is ObjectNode) {
                val value = updateNode.get(fieldName)
                mainNode.put(fieldName, value)
            }
        }
    }
    return mainNode
}