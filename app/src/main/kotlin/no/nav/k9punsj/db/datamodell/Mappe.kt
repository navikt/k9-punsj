package no.nav.k9punsj.db.datamodell


import no.nav.k9punsj.rest.web.JournalpostId
import no.nav.k9punsj.rest.web.SøknadJson

typealias MappeId = String

data class Mappe(
    val mappeId: MappeId,
    val søker: Person,
    val bunke: List<BunkeEntitet>,
)

data class PersonInfo(
    val innsendinger: MutableSet<JournalpostId>,
    val soeknad: SøknadJson,
)






