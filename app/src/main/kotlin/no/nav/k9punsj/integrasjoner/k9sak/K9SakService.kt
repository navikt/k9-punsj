package no.nav.k9punsj.integrasjoner.k9sak

import no.nav.k9punsj.db.datamodell.FagsakYtelseType
import no.nav.k9punsj.domenetjenester.dto.ArbeidsgiverMedArbeidsforholdId
import no.nav.k9punsj.domenetjenester.dto.PeriodeDto

interface K9SakService {

    suspend fun hentPerioderSomFinnesIK9(søker: String, barn: String, fagsakYtelseType: FagsakYtelseType) : Pair<List<PeriodeDto>?, String?>

    suspend fun hentArbeidsforholdIdFraInntektsmeldinger(søker: String, fagsakYtelseType: FagsakYtelseType, periodeDto: PeriodeDto) : Pair<List<ArbeidsgiverMedArbeidsforholdId>?, String?>

    suspend fun hentFagsaker(søker: String): Pair<Set<Fagsak>?, String?>
}
