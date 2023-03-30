package no.nav.k9punsj.journalpost.dto

import java.time.LocalDate

data class BehandlingsAarDto(val behandlingsAar: Int = LocalDate.now().year)
