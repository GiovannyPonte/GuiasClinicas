package com.gio.guiasclinicas.ui.components.image

import android.graphics.Bitmap
import android.util.LruCache
import kotlin.math.max

object ImageMemoryCache {
    private val cache: LruCache<String, Bitmap> by lazy {
        // ~1/8 de la memoria disponible para la app
        val maxKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheKb = max(64 * 1024, maxKb / 8) // >= 64MB o 1/8, lo que sea mayor
        object : LruCache<String, Bitmap>(cacheKb) {
            override fun sizeOf(key: String, value: Bitmap): Int {
                return (value.byteCount / 1024)
            }
        }
    }

    private fun bucket(widthPx: Int): Int {
        // redondea a múltiplos de 128 para no crear demasiadas variantes
        val step = 128
        return ((widthPx + step - 1) / step) * step
    }

    fun makeKey(path: String, targetWidthPx: Int) = "$path@w${bucket(targetWidthPx)}"

    fun get(key: String): Bitmap? = cache.get(key)

    fun put(key: String, bitmap: Bitmap) {
        if (get(key) == null) cache.put(key, bitmap)
    }

    fun clear() = cache.evictAll()
}
