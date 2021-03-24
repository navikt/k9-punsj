package no.nav.k9punsj.utils

import java.time.LocalDateTime

class Cache <T>(val cacheSize : Int = 1000){
    private val map =
        object : LinkedHashMap<String, CacheObject<T>>(
        ) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheObject<T>>): Boolean {
                return size > cacheSize
            }
        }

    fun set(key: String, value: CacheObject<T>) {
        map[key] = value
    }

    fun setIfEmpty(key: String, value: CacheObject<T>): Boolean {
        if (get(key) != null) {
            return false
        }
        map[key] = value
        return true
    }

    fun remove(key: String) = map.remove(key)

    fun get(key: String): CacheObject<T>? {
        val cacheObject = map[key] ?: return null
        if (cacheObject.expire.isBefore(LocalDateTime.now())) {
            remove(key)
            return null
        }
        return cacheObject
    }

    fun clear() {
        map.clear()
    }

    fun getKeys(): List<String> {
       return map.keys.filter { get(it) != null }.toList()
    }
}
