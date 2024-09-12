package no.nav.k9punsj.journalpost

import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9punsj.felles.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.k9punsj.felles.JournalpostId
import no.nav.k9punsj.felles.PunsjFagsakYtelseType
import no.nav.k9punsj.fordel.K9FordelType
import no.nav.k9punsj.integrasjoner.dokarkiv.DokarkivGateway
import no.nav.k9punsj.integrasjoner.dokarkiv.SafDtos
import no.nav.k9punsj.integrasjoner.dokarkiv.SafGateway
import no.nav.k9punsj.integrasjoner.k9sak.K9SakService
import no.nav.k9punsj.integrasjoner.k9sak.dto.HentK9SaksnummerGrunnlag
import no.nav.k9punsj.journalpost.dto.KopierJournalpostDto
import no.nav.k9punsj.journalpost.dto.PunsjJournalpost
import no.nav.k9punsj.journalpost.dto.utledK9sakFagsakYtelseType
import no.nav.k9punsj.utils.PeriodeUtils.somPeriodeDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class JournalpostkopieringService(
    private val journalpostRepository: JournalpostRepository,
    private val k9SakService: K9SakService,
    private val safGateway: SafGateway,
    private val dokarkivGateway: DokarkivGateway,
) {
    private companion object {
        private val logger = LoggerFactory.getLogger(JournalpostkopieringService::class.java)
    }
    internal suspend fun kopierJournalpost(
        journalpostId: JournalpostId,
        kopierJournalpostDto: KopierJournalpostDto,
    ): JournalpostId {
        val journalpost = journalpostRepository.hentHvis(journalpostId.toString())
        val (safJournalpost, k9FagsakYtelseType: FagsakYtelseType) = validerJournalpostKopiering(
            journalpost = journalpost,
            journalpostId = journalpostId,
            kopierJournalpostDto = kopierJournalpostDto
        )

        val k9SakGrunnlag = kopierJournalpostDto.somK9SakGrunnlag(
            k9FagsakYtelseType = k9FagsakYtelseType,
            periodeDto = safJournalpost.datoOpprettet.toLocalDate().somPeriodeDto()
        )

        val saksnummer = hentEllerOpprettSaksnummer(
            journalpostId = journalpostId.toString(),
            kopierJournalpostDto = kopierJournalpostDto,
            k9SakGrunnlag = k9SakGrunnlag
        )

        val nyJournalpostId = dokarkivGateway.knyttTilAnnenSak(
            journalpostId = journalpostId,
            identitetsnummer = k9SakGrunnlag.søker.somIdentitetsnummer(),
            saksnummer = saksnummer
        )
        logger.info("Kopiert journalpost: $journalpostId til ny journalpost: $nyJournalpostId med saksnummer: $saksnummer")
        return nyJournalpostId
    }

    private suspend fun validerJournalpostKopiering(
        journalpost: PunsjJournalpost?,
        journalpostId: JournalpostId,
        kopierJournalpostDto: KopierJournalpostDto,
    ): Pair<SafDtos.Journalpost, FagsakYtelseType> {
        checkNotNull(journalpost) { "Finner ikke journalpost." }

        val safJournalpost =  safGateway.hentJournalpostInfo(journalpostId.toString())
        requireNotNull(safJournalpost) { "Finner ikke SAF journalpost." }

        check(!safJournalpost.erUtgående) {
            throw IllegalStateException("Ikke støttet journalposttype: ${safJournalpost.journalposttype}")
        }

        check(safJournalpost.erFerdigstilt) {
            throw IllegalStateException("Journalpost må ferdigstilles før den kopieres. Type: (${safJournalpost.journalposttype}) Status: (${safJournalpost.journalstatus})")
        }

        check(journalpost.type != null && journalpost.type != K9FordelType.INNTEKTSMELDING_UTGÅTT.kode) {
            throw IllegalStateException("Kan ikke kopier journalpost med type inntektsmelding utgått.")
        }

        val k9FagsakYtelseType: FagsakYtelseType =
            kopierJournalpostDto.ytelse?.somK9FagsakYtelseType() ?: journalpost.ytelse?.let {
                val punsjFagsakYtelseType = PunsjFagsakYtelseType.fromKode(it)
                journalpost.utledK9sakFagsakYtelseType(punsjFagsakYtelseType.somK9FagsakYtelseType())
            } ?: throw JournalpostRoutes.KanIkkeKopieresErrorResponse("Mangler ytelse for journalpost.")

        val støttedeYtelseTyperForKopiering = listOf(
            FagsakYtelseType.OMSORGSPENGER_KS,
            FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
            FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE
        )

        check(støttedeYtelseTyperForKopiering.contains(k9FagsakYtelseType)) {
            throw IllegalStateException("Støtter ikke kopiering av ${k9FagsakYtelseType.navn} for relaterte journalposter")
        }

        check(safJournalpost.kanKopieres) {
            "Kan ikke kopieres. $journalpost."
        }
        return Pair(safJournalpost, k9FagsakYtelseType)
    }

    private suspend fun hentEllerOpprettSaksnummer(journalpostId: String, kopierJournalpostDto: KopierJournalpostDto, k9SakGrunnlag: HentK9SaksnummerGrunnlag): String {
        // Henter eller oppretter saksnummer for den originale personen
        val fraSaksnummer = k9SakService.hentEllerOpprettSaksnummer(k9SakGrunnlag)

        // Sjekker om journalposten kopieres til samme person og logger hvis så er tilfelle
        if (kopierJournalpostDto.fra == kopierJournalpostDto.til) {
            logger.info("Kopierer journalpost: $journalpostId til samme person.")
            return fraSaksnummer // Bruker eksisterende saksnummer for samme person
        }

        // Hvis journalposten kopieres til en annen person, Hent eller opprett nytt saksnummer
        val nySaksnummer = k9SakService.hentEllerOpprettSaksnummer(
            k9SakGrunnlag.copy(søker = kopierJournalpostDto.til)
        )
        logger.info("Kopierer journalpost: $journalpostId til ny person med saksnummer: $nySaksnummer")
        return nySaksnummer
    }
}
