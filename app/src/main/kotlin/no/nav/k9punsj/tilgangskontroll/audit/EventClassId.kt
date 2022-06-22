package no.nav.k9punsj.tilgangskontroll.audit

enum class EventClassId(val cefKode: String) {
    /** Bruker har sett data.  */
    AUDIT_ACCESS("audit:access"),

    /** Minimalt innsyn, f.eks. ved visning i liste.  */
    AUDIT_SEARCH("audit:search"),

    /** Bruker har lagt inn nye data  */
    AUDIT_CREATE("audit:create"),

    /** Bruker har endret data  */
    AUDIT_UPDATE("audit:update");
}
