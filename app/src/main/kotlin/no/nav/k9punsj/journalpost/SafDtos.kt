package no.nav.k9punsj.journalpost

import java.time.LocalDateTime

internal object SafDtos {
    internal open class GraphqlQuery(val query: String, val variables: Any? = null)
    internal data class JournalpostQuery(val journalpostId: String) : GraphqlQuery(
        query = """ 
            query {
              journalpost(journalpostId: "$journalpostId") {
                tema
                journalposttype
                relevanteDatoer {
                  dato
                  datotype
                }
                journalstatus
                bruker {
                  type
                  id
                }
                dokumenter {
                  dokumentInfoId
                  dokumentvarianter {
                    variantformat
                    saksbehandlerHarTilgang
                  }
                }
                avsenderMottaker {
                  id
                  type
                }
              }
            }
            """.trimIndent(),
        variables = null
    )

    /*
        Her fremkommer kun verdier vi håndterer i Punsj.
        For å se komplett dokumentasjon på alle gyldige verdier.
        https://saf-q1.nais.preprod.local/graphiql
     */

    internal enum class VariantFormat(beskrivelse: String) {
        ARKIV("Mennesklig leselig variant av dokumentet")
    }

    internal enum class Tema(beskrivelse: String) {
        OMS("Journalposter tilknyttet Kapittel 9 ytelsene (OMS)")
    }

    internal enum class JournalpostType(beskrivelse: String) {
        I("Inngående")
    }

    internal enum class AvsenderType(beskrivelse: String) {
        FNR("Fødselsnummer")
    }

    internal enum class BrukerType(beskrivelse: String) {
        FNR("Fødselsnummer"),
        AKTOERID("AKTOERID")
    }

    internal enum class AvsenderMottakertype(beskrivelse: String) {
        FNR("Fødselsnummer")
    }

    internal enum class Journalstatus(beskrivelse: String) {
        MOTTATT("Mottatt journalpost")
    }

    internal data class Bruker(
        val id: String?,
        val type: String?,
    )

    internal data class Avsender(
        val id: String?,
        val type: String?,
    )

    internal data class AvsenderMottaker(
        val id: String?,
        val type: String?,
    )

    internal data class DokumentVariant(
        val variantformat: String,
        val saksbehandlerHarTilgang: Boolean,
    )

    internal data class Dokument(
        val dokumentInfoId: String,
        val dokumentvarianter: MutableList<DokumentVariant>?,
    )

    internal data class Journalpost(
        val tema: String?,
        val journalposttype: String,
        val journalstatus: String?,
        val bruker: Bruker?,
        val avsender: Avsender?,
        val avsenderMottaker: AvsenderMottaker?,
        val dokumenter: List<Dokument>,
        val relevanteDatoer: List<RelevantDato>,
    )

    internal data class RelevantDato(
        val dato: LocalDateTime,
        val datotype: Datotype,
    )

    internal enum class Datotype(beskrivelse: String) {
        DATO_OPPRETTET("DATO_OPPRETTET"),
        DATO_SENDT_PRINT("DATO_SENDT_PRINT"),
        DATO_EKSPEDERT("DATO_EKSPEDERT"),
        DATO_JOURNALFOERT("DATO_JOURNALFOERT"),
        DATO_REGISTRERT("DATO_REGISTRERT"),
        DATO_AVS_RETUR("DATO_AVS_RETUR"),
        DATO_DOKUMENT("DATO_DOKUMENT");
    }

    internal data class JournalpostResponse(
        val journalpost: Journalpost?,
    )

    data class JournalpostResponseWrapper(
        val data: JournalpostResponse?,
        val errors: List<Any>?,
    ) {
        internal val journalpostFinnesIkke = errors?.toString()?.contains("ikke funnet") ?: false
        internal val manglerTilgang = errors?.toString()?.contains("har ikke tilgang") ?: false
        internal val abacException = errors?.toString()?.contains("AbacException") ?: false
    }
}
