package no.nav.k9punsj.akjonspunkter

import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.fordel.PunsjEventDto
import no.nav.k9punsj.journalpost.Journalpost
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.journalpost.VentDto
import no.nav.k9punsj.kafka.HendelseProducer
import no.nav.k9punsj.kafka.Topics
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.web.dto.AktørIdDto
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
) : AksjonspunktService {

    private companion object {
        private val log: Logger = LoggerFactory.getLogger(AksjonspunktServiceImpl::class.java)
    }

    override suspend fun opprettAksjonspunktOgSendTilK9Los(
        journalpost: Journalpost,
        aksjonspunkt: Pair<AksjonspunktKode, AksjonspunktStatus>,
    ) {
        val eksternId = journalpost.uuid
        val aksjonspunktEntitet = AksjonspunktEntitet(
            aksjonspunktId = UUID.randomUUID().toString(),
            aksjonspunktKode = aksjonspunkt.first,
            journalpostId = journalpost.journalpostId,
            aksjonspunktStatus = aksjonspunkt.second)

        hendelseProducer.sendMedOnSuccess(
            Topics.SEND_AKSJONSPUNKTHENDELSE_TIL_K9LOS,
            lagPunsjDto(eksternId,
                journalpost.journalpostId,
                journalpost.aktørId,
                mutableMapOf(aksjonspunkt.first.kode to aksjonspunkt.second.kode)
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
    ) {
        val journalpost = journalpostRepository.hent(journalpostId)
        val eksternId = journalpost.uuid
        val aksjonspunktEntitet = aksjonspunktRepository.hentAksjonspunkt(journalpostId, aksjonspunkt.first.kode)!!

        hendelseProducer.sendMedOnSuccess(
            Topics.SEND_AKSJONSPUNKTHENDELSE_TIL_K9LOS,
            lagPunsjDto(eksternId,
                journalpostId,
                journalpost.aktørId,
                mutableMapOf(aksjonspunkt.first.kode to aksjonspunkt.second.kode)
            ),
            eksternId.toString()) {

            runBlocking {
                aksjonspunktRepository.settStatus(aksjonspunktEntitet.aksjonspunktId, AksjonspunktStatus.UTFØRT)
                log.info("Setter aksjonspunkt(" + aksjonspunktEntitet.aksjonspunktId + ") med kode (" + aksjonspunktEntitet.aksjonspunktKode.kode + ") til UTFØRT")
            }
        }
    }

    override suspend fun settUtførtPåAltSendLukkOppgaveTilK9Los(journalpostId: String) {
        val aksjonspunkterSomSkalLukkes = aksjonspunktRepository.hentAlleAksjonspunkter(journalpostId)
            .filter { it.aksjonspunktStatus == AksjonspunktStatus.OPPRETTET }

        if (aksjonspunkterSomSkalLukkes.isNullOrEmpty()) {
            val mutableMap = mutableMapOf<String, String>()
            aksjonspunkterSomSkalLukkes.forEach {
                mutableMap.plus(Pair(it.aksjonspunktKode.kode, AksjonspunktStatus.UTFØRT))
            }

            val journalpost = journalpostRepository.hent(journalpostId)
            val eksternId = journalpost.uuid
            hendelseProducer.sendMedOnSuccess(
                Topics.SEND_AKSJONSPUNKTHENDELSE_TIL_K9LOS,
                lagPunsjDto(
                    eksternId,
                    journalpostId,
                    journalpost.aktørId,
                    mutableMap
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

    override suspend fun settUtførtPåAltSendLukkOppgaveTilK9Los(journalpostId: List<String>) {
        journalpostId.forEach { settUtførtPåAltSendLukkOppgaveTilK9Los(it) }
    }

    override suspend fun settUtførtForAksjonspunkterOgSendLukkOppgaveTilK9Los(
        journalpostId: List<String>,
        aksjonspunkt: Pair<AksjonspunktKode, AksjonspunktStatus>,
    ) {
        journalpostId.forEach { settUtførtAksjonspunktOgSendLukkOppgaveTilK9Los(it, aksjonspunkt) }
    }

    override suspend fun sjekkOmDenErPåVent(journalpostId: String): VentDto? {
        val aksjonspunkt =
            aksjonspunktRepository.hentAksjonspunkt(journalpostId, AksjonspunktKode.VENTER_PÅ_INFORMASJON.kode)
        if (aksjonspunkt != null && aksjonspunkt.aksjonspunktStatus == AksjonspunktStatus.OPPRETTET) {
            return VentDto(aksjonspunkt.vent_årsak?.navn!!, aksjonspunkt.frist_tid!!.toLocalDate())
        }
        return null
    }

    override suspend fun settPåVentOgSendTilLos(journalpostId: String) {
        val journalpost = journalpostRepository.hent(journalpostId)
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
                    aktørId = journalpost.aktørId,
                    aksjonspunkter = mutableMapOf(
                        AksjonspunktKode.PUNSJ.kode to AksjonspunktStatus.UTFØRT.kode,
                        AksjonspunktKode.VENTER_PÅ_INFORMASJON.kode to AksjonspunktStatus.OPPRETTET.kode
                    )
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
                    Topics.SEND_AKSJONSPUNKTHENDELSE_TIL_K9LOS,
                    lagPunsjDto(eksternId,
                        journalpostId,
                        journalpost.aktørId,
                        mutableMapOf(
                            AksjonspunktKode.VENTER_PÅ_INFORMASJON.kode to AksjonspunktStatus.OPPRETTET.kode
                        )
                    ),
                    eksternId.toString()) {

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

    private fun lagPunsjDto(
        eksternId: UUID,
        journalpostId: String,
        aktørId: AktørIdDto?,
        aksjonspunkter: MutableMap<String, String>,
    ): String {
        val punsjEventDto = PunsjEventDto(
            eksternId.toString(),
            journalpostId = journalpostId,
            eventTid = LocalDateTime.now(),
            aktørId = aktørId,
            aksjonspunktKoderMedStatusListe = aksjonspunkter
        )
        return objectMapper().writeValueAsString(punsjEventDto)
    }
}
