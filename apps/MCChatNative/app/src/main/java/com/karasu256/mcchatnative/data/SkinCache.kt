package com.karasu256.mcchatnative.data

import android.content.Context
import android.util.LruCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

object SkinCache {
    private val memory = LruCache<String, ByteArray>(64)
    private val _version = MutableStateFlow(0)
    private var directory: File? = null

    val version: StateFlow<Int> = _version.asStateFlow()

    fun init(context: Context) {
        if (directory != null) return
        directory = File(context.applicationContext.cacheDir, "skins").also { it.mkdirs() }
    }

    fun put(userId: String, png: ByteArray) {
        if (memory.get(userId)?.contentEquals(png) == true) return
        memory.put(userId, png)
        directory?.let { File(it, "$userId.png").writeBytes(png) }
        _version.update { it + 1 }
    }

    fun get(userId: String): ByteArray? =
        memory.get(userId) ?: directory?.let { File(it, "$userId.png") }
            ?.takeIf { it.isFile }
            ?.readBytes()
            ?.also { memory.put(userId, it) }
}
