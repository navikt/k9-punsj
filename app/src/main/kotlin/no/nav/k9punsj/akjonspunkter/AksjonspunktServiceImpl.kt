package no.nav.k9punsj.akjonspunkter

import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.fordel.PunsjEventDto
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.kafka.HendelseProducer
import no.nav.k9punsj.kafka.Topics
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.web.dto.AktørIdDto
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class AksjonspunktServiceImpl(
    val hendelseProducer: HendelseProducer,
    val journalpostRepository: JournalpostRepository,
    val aksjonspunktRepository: AksjonspunktRepository,
) : AksjonspunktService {

    override suspend fun opprettAksjonspunktOgSendTilK9Los(
        journalpostId: String,
        aksjonspunkt: Pair<AksjonspunktKode, AksjonspunktStatus>,
    ) {
        val journalpost = journalpostRepository.hent(journalpostId)
        val eksternId = journalpost.uuid
        val aksjonspunktEntitet = AksjonspunktEntitet(
            aksjonspunktId = UUID.randomUUID().toString(),
            aksjonspunktKode = aksjonspunkt.first,
            journalpostId = journalpost.journalpostId,
            aksjonspunktStatus = aksjonspunkt.second)

        hendelseProducer.sendMedOnSuccess(
            Topics.SEND_AKSJONSPUNKTHENDELSE_TIL_K9LOS,
            lagPunsjDto(eksternId,
                journalpostId,
                journalpost.aktørId,
                mutableMapOf(aksjonspunkt.first.kode to aksjonspunkt.second.kode)
            ),
            eksternId.toString()) {

            runBlocking {
                aksjonspunktRepository.opprettAksjonspunkt(aksjonspunktEntitet)
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
            }
        }
    }

    override suspend fun settUtførtForAksjonspunkterOgSendLukkOppgaveTilK9Los(
        journalpostId: List<String>,
        aksjonspunkt: Pair<AksjonspunktKode, AksjonspunktStatus>,
    ) {
        journalpostId.forEach { settUtførtAksjonspunktOgSendLukkOppgaveTilK9Los(it, aksjonspunkt) }
    }

    override suspend fun settPåVent(journalpostId: String) {
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

        if (nåVærendeAp != null) {
            hendelseProducer.sendMedOnSuccess(
                Topics.SEND_AKSJONSPUNKTHENDELSE_TIL_K9LOS,
                lagPunsjDto(eksternId,
                    journalpostId,
                    journalpost.aktørId,
                    mutableMapOf(AksjonspunktKode.PUNSJ.kode to AksjonspunktStatus.UTFØRT.kode)
                ),
                eksternId.toString()) {

                runBlocking {
                    aksjonspunktRepository.settStatus(nåVærendeAp.aksjonspunktId, AksjonspunktStatus.UTFØRT)
                    aksjonspunktRepository.opprettAksjonspunkt(aksjonspunktEntitet)
                }
            }
        } else {
            // inntreffer der man går manuelt inn i punsj og ønsker å sette noe på vent
            aksjonspunktRepository.opprettAksjonspunkt(aksjonspunktEntitet)
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
