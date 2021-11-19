package no.nav.k9punsj.domenetjenester.mappers

import no.nav.k9.formidling.kontrakt.hendelse.Dokumentbestilling
import no.nav.k9.søknad.felles.Feil
import no.nav.k9punsj.brev.DokumentbestillingDto
import no.nav.k9punsj.rest.web.JournalpostId
import org.slf4j.LoggerFactory

internal class MapDokumentTilK9Formidling(
    journalpostId: JournalpostId,
    dto: DokumentbestillingDto,
) {

    private val bestilling = Dokumentbestilling()
    private val feil = mutableListOf<Feil>()


    init {
        kotlin.runCatching {
            dto.eksternReferanse.mapEksternRefernase()
            dto.dokumentbestillingId.mapDokumentbestillingId()
            dto.saksnummer.mapSaksnummer()


            feil.addAll(validator.validate(bestilling).map { Feil(it.propertyPath.toString(), "kode", it.message) })
        }
    }

    internal fun bestilling() = bestilling
    internal fun feil() = feil.toList()
    internal fun bestillingOgFeil() = bestilling() to feil()


    private fun String.mapEksternRefernase() {
        bestilling.eksternReferanse = this

    }

    private fun String.mapDokumentbestillingId() {
        bestilling.dokumentbestillingId = this
    }

    private fun String?.mapSaksnummer() {
        bestilling.saksnummer = this ?: "GSAK"
    }


    internal companion object {
        private val logger = LoggerFactory.getLogger(MapDokumentTilK9Formidling::class.java)
        private val validator = javax.validation.Validation.buildDefaultValidatorFactory().validator

    }
}
