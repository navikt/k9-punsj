package no.nav.k9punsj.felles.dto

import java.time.LocalDate
import java.time.LocalTime

interface SøknadDto {
    val soeknadId: String
    val soekerId: String?
    val mottattDato: LocalDate?
    val klokkeslett: LocalTime?
    val journalposter: List<String>?
}