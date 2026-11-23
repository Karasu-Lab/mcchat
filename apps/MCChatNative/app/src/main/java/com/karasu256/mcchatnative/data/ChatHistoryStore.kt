package com.karasu256.mcchatnative.data

import android.content.Context
import androidx.datastore.core.DataStore
import com.karasu256.mcchatnative.model.ChatHistoryData
import com.karasu256.mcchatnative.model.ChatMessage
import com.karasu256.mcchatnative.model.StoredMessage
import com.karasu256.mcchatnative.model.User
import com.karasu256.mcchatnative.model.UserId
import kotlinx.coroutines.flow.first

object ChatHistoryStore {
    private var store: DataStore<ChatHistoryData>? = null

    @Synchronized
    fun init(context: Context) {
        if (store != null) return
        store = DataStores.get(
            context,
            "chat_history.json",
            JsonOkioSerializer(ChatHistoryData.serializer(), ChatHistoryData()),
        )
    }

    suspend fun load(host: String): List<ChatMessage> =
        store?.data?.first()?.histories?.get(host).orEmpty().map {
            ChatMessage(
                id = it.id,
                sender = User(UserId(it.senderId), it.senderName),
                body = it.body,
                timestampMillis = it.timestampMillis,
                replyId = it.replyId,
                isReply = it.isReply,
                isWorldChange = it.isWorldChange,
            )
        }

    suspend fun clearedAt(host: String): Long = store?.data?.first()?.clearedAt?.get(host) ?: 0L

    suspend fun clear(host: String) {
        store?.updateData {
            it.copy(
                histories = it.histories + (host to emptyList()),
                clearedAt = it.clearedAt + (host to System.currentTimeMillis()),
            )
        }
    }

    suspend fun save(host: String, messages: List<ChatMessage>) {
        val stored = messages.map {
            StoredMessage(
                id = it.id,
                senderId = it.sender.id.value,
                senderName = it.sender.name,
                body = it.body,
                timestampMillis = it.timestampMillis,
                replyId = it.replyId,
                isReply = it.isReply,
                isWorldChange = it.isWorldChange,
            )
        }
        store?.updateData { it.copy(histories = it.histories + (host to stored)) }
    }
}
