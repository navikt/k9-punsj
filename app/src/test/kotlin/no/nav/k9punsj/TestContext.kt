package no.nav.k9punsj

import no.nav.k9.søknad.Søknad
import no.nav.k9punsj.akjonspunkter.AksjonspunktKode
import no.nav.k9punsj.akjonspunkter.AksjonspunktService
import no.nav.k9punsj.akjonspunkter.AksjonspunktStatus
import no.nav.k9punsj.db.datamodell.FagsakYtelseType
import no.nav.k9punsj.db.datamodell.NorskIdent
import no.nav.k9punsj.journalpost.Journalpost
import no.nav.k9punsj.journalpost.VentDto
import no.nav.k9punsj.kafka.HendelseProducer
import no.nav.k9punsj.rest.eksternt.k9sak.K9SakService
import no.nav.k9punsj.rest.eksternt.pdl.IdentPdl
import no.nav.k9punsj.rest.eksternt.pdl.PdlResponse
import no.nav.k9punsj.rest.eksternt.pdl.PdlService
import no.nav.k9punsj.rest.eksternt.pdl.Personopplysninger
import no.nav.k9punsj.rest.eksternt.punsjbollen.PunsjbolleRuting
import no.nav.k9punsj.rest.eksternt.punsjbollen.PunsjbolleService
import no.nav.k9punsj.rest.info.IIdToken
import no.nav.k9punsj.rest.info.ITokenService
import no.nav.k9punsj.rest.info.IdTokenLocal
import no.nav.k9punsj.rest.web.dto.*
import no.nav.k9punsj.util.DatabaseUtil
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import java.time.LocalDate
import javax.sql.DataSource

@TestConfiguration
@TestProfil
class TestContext {

    val dummyFnr = "11111111111"
    val dummyAktørId = "1000000000000"
    val dummySaksnummer = "133742069666"

    @Bean
    fun hendelseProducerBean() = hendelseProducerMock
    val hendelseProducerMock: HendelseProducer = object : HendelseProducer {
        override fun send(topicName: String, data: String, key: String) {
        }

        override fun sendMedOnSuccess(topicName: String, data: String, key: String, onSuccess: () -> Unit) {

        }
    }

    @Bean
    fun tokenServiceBean() = tokenService
    val tokenService: ITokenService = object : ITokenService {
        override fun decodeToken(accessToken: String): IIdToken {
            return IdTokenLocal()
        }
    }

    @Bean
    fun aksjonspunktService() = aksjonspunktService
    val aksjonspunktService: AksjonspunktService = object : AksjonspunktService {
        override suspend fun opprettAksjonspunktOgSendTilK9Los(
            journalpost: Journalpost,
            aksjonspunkt: Pair<AksjonspunktKode, AksjonspunktStatus>,
            type: String?,
            ytelse: String?,
        ) {
        }

        override suspend fun settUtførtAksjonspunktOgSendLukkOppgaveTilK9Los(
            journalpostId: String,
            aksjonspunkt: Pair<AksjonspunktKode, AksjonspunktStatus>,
        ) {

        }

        override suspend fun settPåVentOgSendTilLos(journalpostId: String, søknadId: SøknadIdDto) {

        }

        override suspend fun settUtførtForAksjonspunkterOgSendLukkOppgaveTilK9Los(
            journalpostId: List<String>,
            aksjonspunkt: Pair<AksjonspunktKode, AksjonspunktStatus>,
        ) {

        }

        override suspend fun sjekkOmDenErPåVent(journalpostId: String): VentDto? {
            return null
        }

        override suspend fun settUtførtPåAltSendLukkOppgaveTilK9Los(journalpostId: List<String>, erSendtInn: Boolean) {
        }

        override suspend fun settUtførtPåAltSendLukkOppgaveTilK9Los(journalpostId: String, erSendtInn: Boolean) {
        }
    }

    @Bean
    fun punsjBolleServiceBean() = punsjbolleService
    val punsjbolleService : PunsjbolleService = object : PunsjbolleService {
        override suspend fun opprettEllerHentFagsaksnummer(
            søker: NorskIdentDto,
            barn: NorskIdentDto,
            journalpostId: JournalpostIdDto?,
            periode: PeriodeDto?,
            correlationId: CorrelationId
        ) = require(journalpostId != null || periode != null) {
            "Må sette minst en av journalpostId og periode"
        }.let { SaksnummerDto(dummySaksnummer) }

        override suspend fun opprettEllerHentFagsaksnummer(
            søker: NorskIdentDto,
            barn: NorskIdentDto,
            søknad: Søknad,
            correlationId: CorrelationId
        ) = SaksnummerDto(dummySaksnummer)

        override suspend fun ruting(
            søker: NorskIdentDto,
            barn: NorskIdentDto,
            journalpostId: JournalpostIdDto?,
            periode: PeriodeDto?,
            correlationId: CorrelationId
        ) = PunsjbolleRuting.K9Sak
    }

    @Bean
    fun pdlServiceBean() = pdlServiceMock
    val pdlServiceMock: PdlService = object : PdlService {
        private val harBarn = "66666666666"
        private val barn = setOf("77777777777","88888888888","99999999999")

        override suspend fun identifikator(fnummer: String): PdlResponse {
            val identer = IdentPdl.Data.HentIdenter.Identer(gruppe = "AKTORID", false, dummyAktørId)
            val identPdl = IdentPdl(IdentPdl.Data(IdentPdl.Data.HentIdenter(identer = listOf(identer))), null)
            return PdlResponse(false, identPdl)
        }

        override suspend fun identifikatorMedAktørId(aktørId: String): PdlResponse {
            val identer = IdentPdl.Data.HentIdenter.Identer(gruppe = "FOLKEREGISTERIDENT", false, dummyFnr)
            val identPdl = IdentPdl(IdentPdl.Data(IdentPdl.Data.HentIdenter(identer = listOf(identer))), null)

            return PdlResponse(false, identPdl)
        }

        override suspend fun aktørIdFor(fnummer: String): String {
            return dummyAktørId
        }

        override suspend fun hentBarn(identitetsnummer: String) = when (identitetsnummer == harBarn) {
            true -> barn
            false -> emptySet()
        }

        override suspend fun hentPersonopplysninger(identitetsnummer: Set<String>) = when {
            identitetsnummer == barn -> setOf(
                Personopplysninger(
                    identitetsnummer = "77777777777",
                    fødselsdato = LocalDate.parse("2005-12-12"),
                    fornavn = "Ola",
                    mellomnavn = null,
                    etternavn = "Nordmann",
                    gradering = Personopplysninger.Gradering.STRENGT_FORTROLIG
                ),
                Personopplysninger(
                    identitetsnummer = "88888888888",
                    fødselsdato = LocalDate.parse("2005-12-12"),
                    fornavn = "Kari",
                    mellomnavn = "Mellomste",
                    etternavn = "Nordmann",
                    gradering = Personopplysninger.Gradering.UGRADERT
                ),
                Personopplysninger(
                    identitetsnummer = "99999999999",
                    fødselsdato = LocalDate.parse("2004-06-24"),
                    fornavn = "Pål",
                    mellomnavn = null,
                    etternavn = "Nordmann",
                    gradering = Personopplysninger.Gradering.UGRADERT
                )
            )
            identitetsnummer == setOf(harBarn) -> setOf(
                Personopplysninger(
                    identitetsnummer = harBarn,
                    fødselsdato = LocalDate.parse("1980-05-06"),
                    fornavn = "Søker",
                    mellomnavn = null,
                    etternavn = "Søkersen",
                    gradering = Personopplysninger.Gradering.STRENGT_FORTROLIG_UTLAND
                )
            )
            else -> setOf()
        }
    }
}
