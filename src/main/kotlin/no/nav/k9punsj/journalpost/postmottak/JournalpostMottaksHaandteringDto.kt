package no.nav.k9punsj.journalpost.postmottak

import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER
import no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_AO
import no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_KS
import no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_MA
import no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER
import no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE
import no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN
import org.springframework.http.HttpStatus

data class JournalpostMottaksHaandteringDto(
    val journalpostId: String,
    val brukerIdent: String,
    val pleietrengendeIdent: String?, // Settes kun ved tilknytning mot reservert saksnummer (hvis man vet det).
    val relatertPersonIdent: String?, // Settes kun ved tilknytning mot reservert saksnummer (hvis man vet det).
    val fagsakYtelseTypeKode: String,
    val saksnummer: String?, // Settes kun ved tilknytning mot eksisterende sak.
) {

    fun valider(behandlingsÅr: Int?): JournalpostMottaksHaandteringDto{
        val ytelseType = FagsakYtelseType.fromString(fagsakYtelseTypeKode)
        when (ytelseType) {
            PLEIEPENGER_SYKT_BARN, PLEIEPENGER_NÆRSTÅENDE -> {} // OK
            OMSORGSPENGER, OMSORGSPENGER_KS, OMSORGSPENGER_MA, OMSORGSPENGER_AO -> {
                if (behandlingsÅr == null || (behandlingsÅr !in 2000..2100)) {
                    throw PostMottakException(
                        "Ugyldig verdi behandlingsÅr ($behandlingsÅr) for ytelseType $fagsakYtelseTypeKode",
                        HttpStatus.BAD_REQUEST,
                        journalpostId
                    )
                }

            }

            OPPLÆRINGSPENGER -> TODO()
            else -> throw PostMottakException(
                "Ikke støttet ytelseType $fagsakYtelseTypeKode",
                HttpStatus.BAD_REQUEST,
                journalpostId
            )
        }
        return this
    }
}
