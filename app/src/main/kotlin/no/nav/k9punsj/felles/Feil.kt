package no.nav.k9punsj.felles

internal class IkkeStøttetJournalpost(feil: String = "Punsj støtter ikke denne journalposten.") : Throwable(feil)
internal class NotatUnderArbeidFeil : Throwable("Notatet må ferdigstilles før det kan åpnes i Punsj")
internal class IkkeTilgang(feil: String) : Throwable(feil)
internal class FeilIAksjonslogg(feil: String) : Throwable(feil)
internal class UgyldigToken(feil: String) : Throwable(feil)
internal class IkkeFunnet : Throwable()
internal class UventetFeil(feil: String) : Throwable(feil)

@Deprecated(
    message = "Standardiseres",
    replaceWith = ReplaceWith("ServerResponse.badRequest().buildAndAwait()")
)
internal class SøknadFinnsIkke(id: String) : Throwable("Søknaden med id=$id finnes ikke")

@Deprecated(
    message = "Standardiseres",
    replaceWith = ReplaceWith("ServerResponse.status(httpStatus).json().bodyValueAndAwait(OasFeil(feilen))")
)
internal class ValideringsFeil(feil: String) : Throwable(feil)