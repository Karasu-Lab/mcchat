package com.karasu256.mcchatnative.data

import android.content.Context
import com.karasu256.mcchat.common.component.ChatText
import com.karasu256.mcchat.common.event.EventRegistry
import com.karasu256.mcchatnative.model.ChatMessage
import com.karasu256.mcchatnative.model.StoredMessage
import com.karasu256.mcchatnative.model.User
import com.karasu256.mcchatnative.model.UserId
import java.util.UUID

object ChatHistoryStore {
    private var dao: AppDao? = null
    private val gson = EventRegistry.common().gson()

    @Synchronized
    fun init(context: Context) {
        if (dao != null) return
        dao = AppDatabase.get(context).dao()
    }

    suspend fun load(host: String): List<ChatMessage> =
        dao?.messages(host).orEmpty().map {
            ChatMessage(
                id = it.id,
                sender = User(UserId(it.senderId), it.senderName),
                body = it.body,
                timestampMillis = it.timestampMillis,
                replyId = it.replyId,
                text = it.text?.let { json -> gson.fromJson(json, ChatText::class.java) }
                    ?: ChatText.raw(it.body),
                eventId = it.eventId?.let(UUID::fromString),
                world = it.world,
            )
        }

    suspend fun clearedAt(host: String): Long = dao?.clearedAt(host) ?: 0L

    suspend fun clear(host: String) {
        dao?.clearMessages(host, System.currentTimeMillis())
    }

    suspend fun save(host: String, messages: List<ChatMessage>) {
        val stored = messages.mapIndexed { index, it ->
            StoredMessage(
                host = host,
                position = index,
                id = it.id,
                senderId = it.sender.id.value,
                senderName = it.sender.name,
                body = it.body,
                timestampMillis = it.timestampMillis,
                replyId = it.replyId,
                text = gson.toJson(it.text),
                eventId = it.eventId?.toString(),
                world = it.world,
            )
        }
        dao?.replaceMessages(host, stored)
    }
}
