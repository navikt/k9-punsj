package no.nav.k9.journalpost

internal object SafDtos {
    internal open class GraphqlQuery(val query: String, val variables: Any? = null)
    internal data class JournalpostQuery(val journalpostId: String) : GraphqlQuery(
            query = """ 
            query {
              journalpost(journalpostId: "$journalpostId") {
                tema
                journalposttype
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
                  idType
                }
              }git restore stag
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
        FNR("Fødselsnummer")
        //AKTOERID("AktørID på enten person eller organisasjon") // TODO: Må støtte at AktørID er satt
    }

    internal enum class AvsenderMottakertype(beskrivelse: String) {
        FNR("Fødselsnummer")
    }

    internal enum class Journalstatus(beskrivelse: String) {
        MOTTATT("Mottatt journalpost")
    }

    internal data class Bruker(
        val id: String?,
        val type: String?
    )

    internal data class Avsender(
        val id: String?,
        val type: String?
    )

    internal data class AvsenderMottaker(
        val id: String?,
        val idType: String?
    )

    internal data class DokumentVariant(
        val variantformat: String,
        val saksbehandlerHarTilgang: Boolean
    )

    internal data class Dokument(
        val dokumentInfoId: String,
        val dokumentvarianter: MutableList<DokumentVariant>?
    )

    internal data class Journalpost(
        val tema: String?,
        val journalposttype: String,
        val journalstatus: String?,
        val bruker: Bruker?,
        val avsender: Avsender?,
        val avsenderMottaker: AvsenderMottaker?,
        val dokumenter: List<Dokument>
    )

    internal data class JournalpostResponse(
        val journalpost: Journalpost?
    )

    data class JournalpostResponseWrapper(
        val data: JournalpostResponse?,
        val errors : List<Any>?
    ) {
        internal val journalpostFinnesIkke = errors?.toString()?.contains("NullPointerException")?:false
        internal val manglerTilgang = errors?.toString()?.contains("AbacException")?:false
    }
}