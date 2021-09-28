package no.nav.k9punsj

import org.springframework.context.annotation.Profile

@Profile("!test & !local")
annotation class StandardProfil