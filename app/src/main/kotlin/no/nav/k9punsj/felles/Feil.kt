package no.nav.k9punsj.felles

internal class IkkeStøttetJournalpost(feil: String = "Punsj støtter ikke denne journalposten.") : Throwable(feil)
internal class NotatUnderArbeidFeil : Throwable("Notatet må ferdigstilles før det kan åpnes i Punsj")
internal class IkkeTilgang(feil: String) : Throwable(feil)
internal class FeilIAksjonslogg(feil: String) : Throwable(feil)
internal class UgyldigToken(feil: String) : Throwable(feil)
internal class IkkeFunnet : Throwable()
internal class UventetFeil(feil: String) : Throwable(feil)