package no.nav.k9punsj.utils

object StringUtils {
    fun String?.erSatt() = !isNullOrBlank()
    fun String.blankAsNull() = when (isBlank()) {
        true -> null
        false -> this
    }
}
