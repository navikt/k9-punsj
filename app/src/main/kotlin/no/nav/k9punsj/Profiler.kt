package no.nav.k9punsj

import org.springframework.context.annotation.Profile

@Profile("!test & !local")
annotation class StandardProfil

@Profile("!test")
annotation class IkkeTestProfil

@Profile("!local")
annotation class IkkeLokalProfil

@Profile("test | local")
annotation class IkkeKlarForProduksjon(
    @Suppress("UNUSED_PARAMETER") val begrunnelse: String
)
