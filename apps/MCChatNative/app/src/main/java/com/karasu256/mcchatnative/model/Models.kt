package com.karasu256.mcchatnative.model

import androidx.annotation.StringRes
import com.karasu256.mcchatnative.R
import kotlinx.serialization.Serializable

data class Client(
    val id: String,
    val name: String,
    val address: String,
    val isOnline: Boolean,
)

@Serializable
data class ConnectionInfo(
    val id: String,
    val host: String,
    val port: Int,
    val userId: String? = null,
    val userName: String? = null,
)

@Serializable
data class ConnectionStoreData(
    val connections: List<ConnectionInfo> = emptyList(),
)

@Serializable
data class StoredMessage(
    val id: Long,
    val senderId: String,
    val senderName: String,
    val body: String,
    val timestampMillis: Long,
    val replyId: Long? = null,
    val isReply: Boolean = false,
)

@Serializable
data class ChatHistoryData(
    val histories: Map<String, List<StoredMessage>> = emptyMap(),
    val clearedAt: Map<String, Long> = emptyMap(),
)

@JvmInline
value class UserId(val value: String)

data class User(val id: UserId, val name: String)

data class ChatMessage(
    val id: Long,
    val sender: User,
    val body: String,
    val timestampMillis: Long,
    val replyId: Long? = null,
    val isReply: Boolean = false,
    val reactions: List<MessageReaction> = emptyList(),
) {
    fun isSentBy(userId: UserId): Boolean = sender.id == userId

    fun isMentioning(userId: UserId, replyTarget: ChatMessage?): Boolean =
        isReply && !isSentBy(userId) && replyTarget?.isSentBy(userId) == true
}

@Serializable
enum class ThreadCategory(@StringRes val labelRes: Int) {
    Pinned(R.string.drawer_pinned),
    Recent(R.string.drawer_recent),
}

@Serializable
data class ChatThread(
    val id: String,
    val title: String,
    val clientId: String,
    val category: ThreadCategory = ThreadCategory.Recent,
    val createdAtMillis: Long,
) {
    val isPinned: Boolean get() = category == ThreadCategory.Pinned
}

@Serializable
data class ThreadStoreData(
    val threads: List<ChatThread> = emptyList(),
)

sealed interface ChatEvent {
    data class Message(
        val id: Long,
        val sender: User,
        val body: String,
        val replyId: Long? = null,
        val isReply: Boolean = false,
        val timestampMillis: Long = System.currentTimeMillis(),
    ) : ChatEvent
    data class Typing(val user: User, val isTyping: Boolean) : ChatEvent
    data class Reaction(val intent: ReactionIntent) : ChatEvent
}

data class DrawerSection(
    val category: ThreadCategory,
    val threads: List<ChatThread>,
)

data class DrawerUser(
    val name: String,
    val subtitle: String,
)
