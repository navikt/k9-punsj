package no.nav.k9punsj.akjonspunkter

import com.fasterxml.jackson.module.kotlin.convertValue
import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.db.datamodell.AktørId
import no.nav.k9punsj.db.repository.SøknadRepository
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.fordel.PunsjEventDto
import no.nav.k9punsj.journalpost.PunsjJournalpost
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.journalpost.VentDto
import no.nav.k9punsj.kafka.HendelseProducer
import no.nav.k9punsj.kafka.Topics
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.pleiepengersyktbarn.PleiepengerSyktBarnSøknadDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class AksjonspunktServiceImpl(
    val hendelseProducer: HendelseProducer,
    val journalpostRepository: JournalpostRepository,
    val aksjonspunktRepository: AksjonspunktRepository,
    val søknadRepository: SøknadRepository,
    val personService: PersonService
) : AksjonspunktService {

    private companion object {
        private val log: Logger = LoggerFactory.getLogger(AksjonspunktServiceImpl::class.java)
    }

    override suspend fun opprettAksjonspunktOgSendTilK9Los(
        punsjJournalpost: PunsjJournalpost,
        aksjonspunkt: Pair<AksjonspunktKode, AksjonspunktStatus>,
        type: String?,
        ytelse: String?,
    ) {
        val (aksjonspunktKode, aksjonspunktStatus) = aksjonspunkt
        val eksternId = punsjJournalpost.uuid
        val aksjonspunktEntitet = AksjonspunktEntitet(
            aksjonspunktId = UUID.randomUUID().toString(),
            aksjonspunktKode = aksjonspunktKode,
            journalpostId = punsjJournalpost.journalpostId,
            aksjonspunktStatus = aksjonspunktStatus)

        hendelseProducer.sendMedOnSuccess(
            Topics.SEND_AKSJONSPUNKTHENDELSE_TIL_K9LOS,
            lagPunsjDto(eksternId = eksternId,
                journalpostId = punsjJournalpost.journalpostId,
                aktørId = punsjJournalpost.aktørId,
                aksjonspunkter = mutableMapOf(aksjonspunktKode.kode to aksjonspunktStatus.kode),
                ytelse = ytelse,
                type = type
            ),
            eksternId.toString()) {

            runBlocking {
                aksjonspunktRepository.opprettAksjonspunkt(aksjonspunktEntitet)
                log.info("Opprettet aksjonspunkt(" + aksjonspunktEntitet.aksjonspunktId + ") med kode (" + aksjonspunktEntitet.aksjonspunktKode.kode + ")")
            }
        }
    }

    override suspend fun settUtførtAksjonspunktOgSendLukkOppgaveTilK9Los(
        journalpostId: String,
        aksjonspunkt: Pair<AksjonspunktKode, AksjonspunktStatus>,
        ansvarligSaksbehandler: String?
    ) {
        val journalpost = journalpostRepository.hent(journalpostId)
        val eksternId = journalpost.uuid
        val (aksjonspunktKode, aksjonspunktStatus) = aksjonspunkt
        val aksjonspunktEntitet = aksjonspunktRepository.hentAksjonspunkt(journalpostId, aksjonspunktKode.kode)!!

        hendelseProducer.sendMedOnSuccess(
            Topics.SEND_AKSJONSPUNKTHENDELSE_TIL_K9LOS,
            lagPunsjDto(eksternId,
                journalpostId,
                journalpost.aktørId,
                mutableMapOf(aksjonspunktKode.kode to aksjonspunktStatus.kode),
                ferdigstiltAv = ansvarligSaksbehandler,
            ),
            eksternId.toString()) {

            runBlocking {
                aksjonspunktRepository.settStatus(aksjonspunktEntitet.aksjonspunktId, AksjonspunktStatus.UTFØRT)
                log.info("Setter aksjonspunkt(" + aksjonspunktEntitet.aksjonspunktId + ") med kode (" + aksjonspunktEntitet.aksjonspunktKode.kode + ") til UTFØRT")
            }
        }
    }

    override suspend fun settUtførtPåAltSendLukkOppgaveTilK9Los(
        journalpostId: String,
        erSendtInn: Boolean,
        ansvarligSaksbehandler: String?
    ) {
        val aksjonspunkterSomSkalLukkes = aksjonspunktRepository.hentAlleAksjonspunkter(journalpostId)
            .filter { it.aksjonspunktStatus == AksjonspunktStatus.OPPRETTET }

        if (aksjonspunkterSomSkalLukkes.isNotEmpty()) {
            val mutableMap = mutableMapOf<String, String>()
            aksjonspunkterSomSkalLukkes.forEach {
                mutableMap.plus(Pair(it.aksjonspunktKode.kode, AksjonspunktStatus.UTFØRT))
            }

            val journalpost = journalpostRepository.hent(journalpostId)
            val eksternId = journalpost.uuid
            hendelseProducer.sendMedOnSuccess(
                Topics.SEND_AKSJONSPUNKTHENDELSE_TIL_K9LOS,
                lagPunsjDto(
                    eksternId = eksternId,
                    journalpostId = journalpostId,
                    aktørId = journalpost.aktørId,
                    aksjonspunkter = mutableMap,
                    sendtInn = erSendtInn,
                    ferdigstiltAv = ansvarligSaksbehandler
                ),
                eksternId.toString()) {
                runBlocking {
                    aksjonspunkterSomSkalLukkes.forEach {
                        aksjonspunktRepository.settStatus(it.aksjonspunktId, AksjonspunktStatus.UTFØRT)
                        log.info("Setter aksjonspunkt(" + it.aksjonspunktId + ") med kode (" + it.aksjonspunktKode.kode + ") til UTFØRT")
                    }
                }
            }
        }
    }

    override suspend fun settUtførtPåAltSendLukkOppgaveTilK9Los(journalpostId: Collection<String>, erSendtInn: Boolean, ansvarligSaksbehandler: String?) {
        journalpostId.forEach { settUtførtPåAltSendLukkOppgaveTilK9Los(it, erSendtInn, ansvarligSaksbehandler) }
    }

    override suspend fun sjekkOmDenErPåVent(journalpostId: String): VentDto? {
        val aksjonspunkt =
            aksjonspunktRepository.hentAksjonspunkt(journalpostId, AksjonspunktKode.VENTER_PÅ_INFORMASJON.kode)
        if (aksjonspunkt != null && aksjonspunkt.aksjonspunktStatus == AksjonspunktStatus.OPPRETTET) {
            return VentDto(aksjonspunkt.vent_årsak?.navn!!, aksjonspunkt.frist_tid!!.toLocalDate())
        }
        return null
    }

    override suspend fun settPåVentOgSendTilLos(journalpostId: String, søknadId: String?) {
        val journalpost = journalpostRepository.hent(journalpostId)
        val søknad = if (søknadId != null) søknadRepository.hentSøknad(søknadId = søknadId)?.søknad else null
        val barnIdent  = if (søknad != null) {
            val vising: PleiepengerSyktBarnSøknadDto = objectMapper().convertValue(søknad)
            val norskIdent = vising.barn?.norskIdent
            norskIdent
        } else null

        val eksternId = journalpost.uuid
        val aksjonspunktEntitet = AksjonspunktEntitet(
            aksjonspunktId = UUID.randomUUID().toString(),
            aksjonspunktKode = AksjonspunktKode.VENTER_PÅ_INFORMASJON,
            journalpostId = journalpost.journalpostId,
            aksjonspunktStatus = AksjonspunktStatus.OPPRETTET,
            //TODO er det riktig med 3 uker?? ta hensyn til røde dager? + helger?
            frist_tid = LocalDateTime.now().plusWeeks(3),
            VentÅrsakType.VENT_TRENGER_FLERE_OPPLYSINGER)

        val nåVærendeAp = aksjonspunktRepository.hentAksjonspunkt(journalpostId, AksjonspunktKode.PUNSJ.kode)

        if (nåVærendeAp != null && nåVærendeAp.aksjonspunktStatus != AksjonspunktStatus.UTFØRT) {
            hendelseProducer.sendMedOnSuccess(
                topicName = Topics.SEND_AKSJONSPUNKTHENDELSE_TIL_K9LOS,
                data = lagPunsjDto(eksternId,
                    journalpostId = journalpostId,
                    aktørId = uteldAktørId(søknadId, journalpost),
                    aksjonspunkter = mutableMapOf(
                        AksjonspunktKode.PUNSJ.kode to AksjonspunktStatus.UTFØRT.kode,
                        AksjonspunktKode.VENTER_PÅ_INFORMASJON.kode to AksjonspunktStatus.OPPRETTET.kode
                    ),
                    barnIdent = barnIdent
                ),
                key = eksternId.toString()) {
                runBlocking {
                    aksjonspunktRepository.settStatus(nåVærendeAp.aksjonspunktId, AksjonspunktStatus.UTFØRT)
                    log.info("Setter aksjonspunkt(" + nåVærendeAp.aksjonspunktId + ") med kode (" + nåVærendeAp.aksjonspunktKode.kode + ") til UTFØRT")
                    aksjonspunktRepository.opprettAksjonspunkt(aksjonspunktEntitet)
                    log.info("Opprettet aksjonspunkt(" + aksjonspunktEntitet.aksjonspunktId + ") med kode (" + aksjonspunktEntitet.aksjonspunktKode.kode + ")")
                }
            }
        } else {
            // inntreffer der man går manuelt inn i punsj og ønsker å sette noe på vent, det finnes altså ingen punsje oppgave opprinnelig
            val ventePunkt =
                aksjonspunktRepository.hentAksjonspunkt(journalpostId, AksjonspunktKode.VENTER_PÅ_INFORMASJON.kode)
            if (ventePunkt != null && ventePunkt.aksjonspunktStatus != AksjonspunktStatus.OPPRETTET) {
                hendelseProducer.sendMedOnSuccess(
                    topicName = Topics.SEND_AKSJONSPUNKTHENDELSE_TIL_K9LOS,
                    data = lagPunsjDto(eksternId,
                        journalpostId = journalpostId,
                        aktørId = uteldAktørId(søknadId, journalpost),
                        aksjonspunkter = mutableMapOf(
                            AksjonspunktKode.VENTER_PÅ_INFORMASJON.kode to AksjonspunktStatus.OPPRETTET.kode
                        ),
                        barnIdent = barnIdent
                    ),
                    key = eksternId.toString()) {
                    runBlocking {
                        aksjonspunktRepository.opprettAksjonspunkt(aksjonspunktEntitet)
                        log.info("Opprettet aksjonspunkt(" + aksjonspunktEntitet.aksjonspunktId + ") med kode (" + aksjonspunktEntitet.aksjonspunktKode.kode + ")")
                    }
                }
            } else {
                log.info("Denne journalposten($journalpostId) venter allerede - venter til ${ventePunkt?.frist_tid}")
            }
        }
    }

    private suspend fun uteldAktørId(søknadId: String?, punsjJournalpost: PunsjJournalpost) : AktørId? {
        if (søknadId == null) {
            return punsjJournalpost.aktørId
        }
        val personId = søknadRepository.hentSøknad(søknadId = søknadId)?.søkerId ?: return punsjJournalpost.aktørId
        val aktørIdPåSøknaden = personService.finnPerson(personId).aktørId

        // betyr at søknaden har byttet fra opprinnelig aktørId til ny aktørId (kan skje hvis den opprinnelig journalposten kommer inn på barnet sitt aktørNummer)
        if (aktørIdPåSøknaden != punsjJournalpost.aktørId) {
            return aktørIdPåSøknaden
        }
        return punsjJournalpost.aktørId
    }

    private fun lagPunsjDto(
        eksternId: UUID,
        journalpostId: String,
        aktørId: String?,
        aksjonspunkter: MutableMap<String, String>,
        ytelse: String? = null,
        type: String? = null,
        barnIdent: String? = null,
        sendtInn : Boolean? = null,
        ferdigstiltAv: String? = null
    ): String {
        val punsjEventDto = PunsjEventDto(
            eksternId.toString(),
            journalpostId = journalpostId,
            eventTid = LocalDateTime.now(),
            aktørId = aktørId,
            aksjonspunktKoderMedStatusListe = aksjonspunkter,
            pleietrengendeAktørId = barnIdent,
            ytelse = ytelse,
            type = type,
            sendtInn = sendtInn,
            ferdigstiltAv = ferdigstiltAv
        )
        return objectMapper().writeValueAsString(punsjEventDto)
    }
}
