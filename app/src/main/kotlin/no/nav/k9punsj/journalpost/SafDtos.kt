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
                tilleggsopplysninger {
                  nokkel
                  verdi
                }
              }
            }
            """.trimIndent(),
        variables = null
    )

    internal data class FerdigstillJournalpostQuery(val journalpostId: String) : GraphqlQuery(
        query = """ 
            query {
              journalpost(journalpostId: "$journalpostId") {
                journalposttype
                journalstatus
                dokumenter {
                  dokumentInfoId
                  tittel
                }
                avsenderMottaker {
                  navn
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

    internal enum class VariantFormat {
        ARKIV
    }

    internal enum class Tema {
        OMS
    }

    enum class JournalpostType {
        I
    }

    internal enum class AvsenderType {
        FNR
    }

    internal enum class BrukerType {
        FNR,
        AKTOERID
    }

    internal enum class AvsenderMottakertype {
        FNR
    }

    internal enum class Journalstatus {
        MOTTATT
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
        private val tilleggsopplysninger: List<Tilleggsopplysning> = emptyList()) {
        val k9Kilde = tilleggsopplysninger.firstOrNull { it.nokkel == "k9.kilde" }?.verdi
        val k9Type = tilleggsopplysninger.firstOrNull { it.nokkel == "k9.type" }?.verdi
        private val erDigital = "DIGITAL" == k9Kilde
        private val erEttersendelse = "ETTERSENDELSE" == k9Type
        private val erSøknad = "SØKNAD" == k9Type
        val erIkkeStøttetDigitalJournalpost = when (erDigital) {
            true -> !(erEttersendelse || erSøknad)
            false -> false
        }
    }

    internal data class Tilleggsopplysning(
        val nokkel: String,
        val verdi: String
    )

    internal data class RelevantDato(
        val dato: LocalDateTime,
        val datotype: Datotype,
    )

    internal enum class Datotype {
        DATO_OPPRETTET,
        DATO_SENDT_PRINT,
        DATO_EKSPEDERT,
        DATO_JOURNALFOERT,
        DATO_REGISTRERT,
        DATO_AVS_RETUR,
        DATO_DOKUMENT;
    }

    internal data class JournalpostResponse(
        val journalpost: Journalpost?,
    )

    data class JournalpostResponseWrapper(
        val data: JournalpostResponse?,
        val errors: List<Any>?) {
        private fun inneholderError(error: String) = errors?.toString()?.contains(error) ?: false
        internal val journalpostFinnesIkke = inneholderError("ikke funnet") || inneholderError("not_found")
        internal val manglerTilgang = inneholderError("har ikke tilgang")
        internal val manglerTilgangPåGrunnAvStatus = manglerTilgang && inneholderError("på grunn av journalposten sin status")
    }
}
