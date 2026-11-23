package com.karasu256.mcchatnative.data

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL

object MojangSkinFetcher {
    suspend fun fetch(uuid: String): ByteArray? = withContext(Dispatchers.IO) {
        val profile = download(
            "https://sessionserver.mojang.com/session/minecraft/profile/${
                uuid.replace(
                    "-",
                    ""
                )
            }"
        )
            ?: return@withContext null
        val textures = Json.parseToJsonElement(profile.decodeToString())
            .jsonObject["properties"]?.jsonArray
            ?.map { it.jsonObject }
            ?.firstOrNull { it["name"]?.jsonPrimitive?.content == "textures" }
            ?.get("value")?.jsonPrimitive?.content
            ?: return@withContext null
        val url = Json.parseToJsonElement(String(Base64.decode(textures, Base64.DEFAULT)))
            .jsonObject["textures"]?.jsonObject
            ?.get("SKIN")?.jsonObject
            ?.get("url")?.jsonPrimitive?.content
            ?.replaceFirst("http://", "https://")
            ?: return@withContext null
        if (URL(url).host != "textures.minecraft.net") return@withContext null
        download(url)
    }

    private fun download(url: String): ByteArray? {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 5_000
        connection.readTimeout = 5_000
        return try {
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.use { it.readBytes() }
            } else {
                null
            }
        } finally {
            connection.disconnect()
        }
    }
}
