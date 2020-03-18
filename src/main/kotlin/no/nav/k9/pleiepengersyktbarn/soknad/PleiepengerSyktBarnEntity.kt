package no.nav.k9.pleiepengersyktbarn.soknad

import java.sql.Date
import java.sql.Timestamp
import java.util.*
import javax.persistence.*

@Entity
data class PleiepengerSyktBarnEntity(
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id_soknad: Int?,
        @Column(nullable = false) val id_mappe: UUID,
        @Column(nullable = false) val norsk_ident: String,
        @Column(nullable = false) val sist_endret: Timestamp,
        @Column(nullable = true) val barn_norsk_ident: String?,
        @Column(nullable = true) val barn_fodselsdato: Date?,
        @Column(nullable = true) val soknad: String?
)/* {
    fun getIdMappe(): UUID {
        return id_mappe;
    }

    fun getNorskIdent(): String {
        return norsk_ident;
    }

    fun getSistEndret(): Timestamp {
        return sist_endret;
    }

    fun getBarnNorskIdent(): String? {
        return barn_norsk_ident;
    }

    fun getBarnFodselsdato(): Date? {
        return barn_fodselsdato;
    }

    fun getSoknad(): String? {
        return soknad;
    }
}*/