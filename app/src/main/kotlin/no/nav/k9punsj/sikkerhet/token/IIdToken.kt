package no.nav.k9punsj.sikkerhet.token


interface IIdToken {
    val value: String
    val jwt: JWTToken?
    fun getName(): String
    fun getUsername(): String
    fun kanBehandleKode6(): Boolean
    fun kanBehandleKode7(): Boolean
    fun kanBehandleEgneAnsatte(): Boolean
    fun erOppgavebehandler(): Boolean
}
