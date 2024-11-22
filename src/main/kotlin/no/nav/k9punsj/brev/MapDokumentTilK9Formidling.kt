package no.nav.k9punsj.brev

import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.validation.Validation.buildDefaultValidatorFactory
import no.nav.k9.formidling.kontrakt.dokumentdataparametre.DokumentdataParametreK9
import no.nav.k9.formidling.kontrakt.dokumentdataparametre.FritekstbrevinnholdDto
import no.nav.k9.formidling.kontrakt.hendelse.Dokumentbestilling
import no.nav.k9.formidling.kontrakt.kodeverk.*
import no.nav.k9.søknad.felles.Feil
import no.nav.k9punsj.brev.dto.BrevDataDto
import no.nav.k9punsj.brev.dto.DokumentbestillingDto
import no.nav.k9punsj.brev.dto.MottakerDto
import no.nav.k9punsj.utils.objectMapper
import org.slf4j.LoggerFactory

internal class MapDokumentTilK9Formidling(
    dto: DokumentbestillingDto,
    aktørId: String
) {

    private val bestilling = Dokumentbestilling()
    private val feil = mutableListOf<Feil>()

    init {
        kotlin.runCatching {
            dto.journalpostId?.leggTilEksternRefernase() ?: dto.saksnummer.leggTilEksternRefernase()
            dto.brevId?.leggTilDokumentbestillingId() // DokumentbestillingId er en unik ID for dokumentbestilling for å håndtere duplikater
            dto.saksnummer.leggTilSaksnummer()
            aktørId.leggTilAktørId()
            dto.mottaker.leggTilMottaker()
            dto.fagsakYtelseType.leggTilFagsakTyelse()
            dto.dokumentMal.leggTilDokumentMal()
            dto.dokumentdata?.leggTilDokumentData()
            bestilling.avsenderApplikasjon = AvsenderApplikasjon.K9PUNSJ
            feil.addAll(validator.validate(bestilling).map {
                Feil(it.propertyPath.toString(), "kode", it.message) }
            )
        }.onFailure { throwable ->
            logger.warn("Uventet mappingfeil", throwable)
            feil.add(Feil("dokumentbestilling", "uventetMappingfeil", throwable.message ?: "Uventet mappingfeil"))
        }
    }

    internal fun bestilling() = bestilling
    internal fun feil() = feil.toList()
    internal fun bestillingOgFeil() = bestilling() to feil()

    private fun String.leggTilEksternRefernase() {
        bestilling.eksternReferanse = this
    }

    private fun String.leggTilDokumentbestillingId() {
        bestilling.dokumentbestillingId = this
    }

    private fun String.leggTilSaksnummer() {
        bestilling.saksnummer = this
    }

    private fun String.leggTilAktørId() {
        bestilling.aktørId = this
    }

    private fun MottakerDto.leggTilMottaker() {
        kotlin.runCatching { Mottaker(this.id, IdType.valueOf(this.type)) }
            .onSuccess { bestilling.overstyrtMottaker = it }
            .onFailure { feil.add(Feil("Mottaker", "Mottaker", it.message)) }
    }

    private fun FagsakYtelseType.leggTilFagsakTyelse() {
        bestilling.ytelseType = this
    }

    private fun String.leggTilDokumentMal() {
        kotlin.runCatching { DokumentMalType.fraKode(this) }
            .onSuccess { bestilling.dokumentMal = it.kode }
            .onFailure { feil.add(Feil("DokumentMalType", "DokumentMalType", it.message)) }
    }

    private fun BrevDataDto.leggTilDokumentData() {
        kotlin.runCatching {
            DokumentdataParametreK9()
            .apply {
                fritekst = this@leggTilDokumentData.fritekst
                fritekstbrev = this@leggTilDokumentData.fritekstbrev?.let { f ->
                    FritekstbrevinnholdDto().apply {
                        overskrift = f.overskrift
                        brødtekst = f.brødtekst
                    }
                }
            }
        }
            .onSuccess { bestilling.dokumentdata = it }
            .onFailure { feil.add(Feil("DokumentData", "DokumentData", it.message)) }
    }

    internal companion object {
        private val logger = LoggerFactory.getLogger(MapDokumentTilK9Formidling::class.java)
        private val validator = buildDefaultValidatorFactory().validator
    }
}
