package no.nav.k9punsj.felles

import org.json.JSONArray
import org.json.JSONObject

internal object JsonUtil {
    fun JSONObject.arrayOrEmptyArray(key: String): JSONArray = when (has(key) && get(key) is JSONArray) {
        true -> getJSONArray(key)
        false -> JSONArray()
    }

    fun JSONObject.objectOrEmptyObject(key: String): JSONObject = when(has(key) && get(key) is JSONObject) {
        true -> getJSONObject(key)
        false -> JSONObject()
    }

    fun JSONObject.stringOrNull(key: String) = when (has(key) && get(key) is String) {
        true -> getString(key)
        else -> null
    }
}