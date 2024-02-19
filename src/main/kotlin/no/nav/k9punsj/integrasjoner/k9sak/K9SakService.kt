package no.nav.k9punsj.integrasjoner.k9sak

import no.nav.k9.kodeverk.dokument.Brevkode
import no.nav.k9.søknad.Søknad
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.felles.dto.ArbeidsgiverMedArbeidsforholdId
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.felles.dto.SaksnummerDto
import no.nav.k9punsj.felles.dto.SøknadEntitet

interface K9SakService {

    suspend fun hentPerioderSomFinnesIK9(
        søker: String,
        barn: String? = null,
        fagsakYtelseType: FagsakYtelseType
    ): Pair<List<PeriodeDto>?, String?>

    suspend fun hentPerioderSomFinnesIK9ForPeriode(
        søker: String,
        barn: String? = null,
        fagsakYtelseType: FagsakYtelseType,
        periode: PeriodeDto
    ): Pair<List<PeriodeDto>?, String?>

    suspend fun hentArbeidsforholdIdFraInntektsmeldinger(
        søker: String,
        fagsakYtelseType: FagsakYtelseType,
        periodeDto: PeriodeDto
    ): Pair<List<ArbeidsgiverMedArbeidsforholdId>?, String?>

    suspend fun hentFagsaker(søker: String): Pair<Set<Fagsak>?, String?>

    suspend fun hentEllerOpprettSaksnummer(
        k9FormatSøknad: Søknad,
        søknadEntitet: SøknadEntitet,
        fagsakYtelseType: FagsakYtelseType
    ): Pair<String?, String?>

    suspend fun sendInnSoeknad(
        soknad: Søknad,
        journalpostId: String,
        fagsakYtelseType: FagsakYtelseType,
        saksnummer: String,
        brevkode: Brevkode
    )

    suspend fun reserverSaksnummer(barnIdent: String?): Pair<SaksnummerDto?, String?>
    suspend fun opprettSakOgSendInnSøknad(
        soknad: Søknad,
        søknadEntitet: SøknadEntitet,
        journalpostId: String,
        fagsakYtelseType: FagsakYtelseType,
        saksnummer: String,
        brevkode: Brevkode,
    )
}
