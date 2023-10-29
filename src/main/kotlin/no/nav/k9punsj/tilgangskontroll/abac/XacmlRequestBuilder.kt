package no.nav.k9punsj.tilgangskontroll.abac

import com.fasterxml.jackson.annotation.JsonAlias

const val ENVIRONMENT_PEP_ID = "no.nav.abac.attributter.environment.felles.pep_id"
const val RESOURCE_DOMENE = "no.nav.abac.attributter.resource.felles.domene"
const val RESOURCE_FNR = "no.nav.abac.attributter.resource.felles.person.fnr"
const val RESOURCE_TYPE = "no.nav.abac.attributter.resource.felles.resource_type"
const val SUBJECTID = "urn:oasis:names:tc:xacml:1.0:subject:subject-id"
const val SUBJECT_TYPE = "no.nav.abac.attributter.subject.felles.subjectType"
const val ACTION_ID = "urn:oasis:names:tc:xacml:1.0:action:action-id"
const val BASIS_TILGANG = "no.nav.abac.attributter.k9"
const val TILGANG_SAK = "no.nav.abac.attributter.k9.fagsak"
const val INTERNBRUKER = "InternBruker"

enum class Category {
    Resource,
    Action,
    Environment,
    AccessSubject,
}

data class CategoryAttribute(
    @JsonAlias("Attribute") var attributes: List<CAttribute> = ArrayList()
)

data class CAttribute(
    @JsonAlias("AttributeId") val attributeId: String,
    @JsonAlias("Value") val value: Any
)

class XacmlRequestBuilder {
    private val requestAttributes = HashMap<Category, CategoryAttribute>()

    fun addResourceAttribute(id: String, value: Any): XacmlRequestBuilder = addAttributeToCategory(Category.Resource, id, value)
    fun addAccessSubjectAttribute(id: String, value: Any): XacmlRequestBuilder = addAttributeToCategory(Category.AccessSubject, id, value)

    fun addEnvironmentAttribute(id: String, value: Any): XacmlRequestBuilder = addAttributeToCategory(Category.Environment, id, value)

    fun addActionAttribute(id: String, value: Any): XacmlRequestBuilder = addAttributeToCategory(Category.Action, id, value)

    fun build(): Map<String, Map<Category, CategoryAttribute>> {
        return mapOf("Request" to requestAttributes)
    }

    private fun addAttributeToCategory(category: Category, id: String, value: Any): XacmlRequestBuilder {
        requestAttributes.getOrPut(category) { CategoryAttribute() }.attributes += CAttribute(id, value)
        return this
    }
}
