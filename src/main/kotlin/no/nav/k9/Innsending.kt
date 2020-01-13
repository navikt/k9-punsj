package no.nav.k9

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.http.HttpStatus
import javax.validation.ConstraintViolation

internal val objectMapper = jacksonObjectMapper()

typealias JournalpostId = String

typealias Søknad = MutableMap<String, Any?>
typealias SøknadType = String
typealias NorskIdent = String

internal fun Søknad.merge(nySøknad: Søknad) : Søknad {
    val før = objectMapper.valueToTree<ObjectNode>(this)
    val nytt = objectMapper.valueToTree<ObjectNode>(nySøknad)
    val merged = objectMapper.convertValue<Søknad>(merge(før, nytt))
    clear()
    putAll(merged)
    return this
}

data class Innsending(
        val personer: Map<NorskIdent, JournalpostInnhold>
)

data class JournalpostInnhold(
        val journalpostId: JournalpostId,
        val soeknad: Søknad
)

data class Mangel(
        val attributt: String,
        val melding: String
)

internal fun Set<ConstraintViolation<*>>.mangler() = map { Mangel(
        attributt = it.propertyPath.toString(),
        melding = it.message
)}.toSet()

internal fun Boolean.httpStatus() = if (this) HttpStatus.OK else HttpStatus.BAD_REQUEST

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