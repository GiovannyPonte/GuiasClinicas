package com.gio.guiasclinicas.util.cache

import android.util.LruCache

/**
 * Simple envoltorio para LruCache que expone getOrPut y clear.
 * No tiene dependencias de Compose y sirve para cualquier tipo.
 */
class SimpleLruCache<K : Any, V : Any>(maxSize: Int) {
    private val lru = object : LruCache<K, V>(maxSize) {}
    @Synchronized
    fun getOrPut(key: K, producer: () -> V): V {
        val existing = lru.get(key)
        if (existing != null) return existing
        val fresh = producer()
        lru.put(key, fresh)
        return fresh
    }
    fun clear() = lru.evictAll()
}
