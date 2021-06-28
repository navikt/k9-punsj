package no.nav.k9punsj.rest.eksternt.pdl

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.k9punsj.rest.eksternt.pdl.Personopplysninger.Gradering.Companion.fraPdlDto
import java.time.LocalDate

internal fun ObjectNode.mapBarnFraRelasjoner() : Set<String> {
    return (this.get("data").get("hentPerson").get("forelderBarnRelasjon") as ArrayNode)
        .map { it as ObjectNode }
        .filter { it.get("relatertPersonsRolle").asText() == "BARN" }
        .map { it.get("relatertPersonsIdent").asText() }
        .toSet()
}

internal fun ObjectNode.mapPersonopplysninger() : Set<Personopplysninger> {
    return (this.get("data").get("hentPersonBolk") as ArrayNode)
        .map { it as ObjectNode }
        .map {
            val person = it.get("person") as ObjectNode
            val navn = (person.get("navn") as ArrayNode).first()
            Personopplysninger(
                identitetsnummer = it.get("ident").asText(),
                fødselsdato = LocalDate.parse((person.get("foedsel") as ArrayNode).first().get("foedselsdato").asText()),
                gradering = (person.get("adressebeskyttelse") as ArrayNode).firstOrNull()?.get("gradering")?.asText().fraPdlDto(),
                fornavn = navn.get("fornavn").asText(),
                mellomnavn = when (navn.hasNonNull("mellomnavn")) {
                    true -> navn.get("mellomnavn").asText()
                    false -> null
                },
                etternavn = navn.get("etternavn").asText()
            )
        }.toSet()
}