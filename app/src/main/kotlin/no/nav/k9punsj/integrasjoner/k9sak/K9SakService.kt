package no.nav.k9punsj.integrasjoner.k9sak

import no.nav.k9punsj.felles.AktørId
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.felles.dto.ArbeidsgiverMedArbeidsforholdId
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.ruting.RutingGrunnlag

interface K9SakService {

    suspend fun hentPerioderSomFinnesIK9(søker: String, barn: String? = null, fagsakYtelseType: FagsakYtelseType): Pair<List<PeriodeDto>?, String?>

    suspend fun hentArbeidsforholdIdFraInntektsmeldinger(søker: String, fagsakYtelseType: FagsakYtelseType, periodeDto: PeriodeDto): Pair<List<ArbeidsgiverMedArbeidsforholdId>?, String?>

    suspend fun hentFagsaker(søker: String): Pair<Set<Fagsak>?, String?>

    suspend fun hentEllerOpprettSaksnummer(k9SaksnummerGrunnlag: HentK9SaksnummerGrunnlag, opprettNytt: Boolean): Pair<String?, String?>

    @Deprecated("Skall ikke brukes, kun for infotrygd-ruting")
    suspend fun harLopendeSakSomInvolvererEnAv(lopendeSakDto: LopendeSakDto): RutingGrunnlag

    @Deprecated("Skall ikke brukes, kun for infotrygd-ruting")
    suspend fun inngårIUnntaksliste(aktørIder: Set<String>): Boolean
}
