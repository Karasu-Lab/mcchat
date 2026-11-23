package com.karasu256.mcchatnative.model

import androidx.annotation.StringRes
import com.karasu256.mcchat.common.component.ChatText
import com.karasu256.mcchatnative.R
import kotlinx.serialization.Serializable
import java.util.UUID

data class Client(
    val id: String,
    val name: String,
    val address: String,
    val isOnline: Boolean,
    val icon: ByteArray? = null,
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
    val text: String? = null,
    val eventId: String? = null,
    val world: World? = null,
)

@Serializable
data class ChatHistoryData(
    val histories: Map<String, List<StoredMessage>> = emptyMap(),
    val clearedAt: Map<String, Long> = emptyMap(),
)

@Serializable
data class World(val id: String, val name: String)

@Serializable
data class WorldStoreData(
    val worlds: List<World> = emptyList(),
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
    val text: ChatText = ChatText.raw(body),
    val eventId: UUID? = null,
    val reactions: List<MessageReaction> = emptyList(),
    val world: World? = null,
) {
    fun isSentBy(userId: UserId): Boolean = sender.id == userId

    fun isMentioning(userId: UserId): Boolean = text.notifies(userId.uuid(), sender.id.uuid())
}

fun UserId.uuid(): UUID? = runCatching { UUID.fromString(value) }.getOrNull()

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
        val text: ChatText,
        val replyId: Long? = null,
        val timestampMillis: Long = System.currentTimeMillis(),
        val eventId: UUID? = null,
    ) : ChatEvent {
        val body: String get() = text.plain()
    }

    data class Delivered(val body: String, val eventId: UUID) : ChatEvent

    data class WorldChanged(
        val id: Long,
        val world: World,
        val timestampMillis: Long,
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
