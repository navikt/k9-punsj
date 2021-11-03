package no.nav.k9punsj.rest.eksternt.k9sak

import no.nav.k9punsj.db.datamodell.FagsakYtelseType
import no.nav.k9punsj.db.datamodell.NorskIdent
import no.nav.k9punsj.rest.web.dto.ArbeidsgiverMedArbeidsforholdId
import no.nav.k9punsj.rest.web.dto.PeriodeDto

interface K9SakService {

    suspend fun hentPerioderSomFinnesIK9(søker: NorskIdent, barn: NorskIdent, fagsakYtelseType: FagsakYtelseType) : Pair<List<PeriodeDto>?, String?>


    suspend fun hentArbeidsforholdIdFraInntektsmeldinger(søker: NorskIdent, fagsakYtelseType: FagsakYtelseType, periodeDto: PeriodeDto) : Pair<List<ArbeidsgiverMedArbeidsforholdId>?, String?>

}
