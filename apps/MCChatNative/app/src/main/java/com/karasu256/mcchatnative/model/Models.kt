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

@JvmInline
value class UserId(val value: String)

data class ChatUser(val id: UserId, val name: String)

data class ChatMessage(
    val id: Long,
    val sender: ChatUser,
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
        val sender: ChatUser,
        val body: String,
        val replyId: Long? = null,
        val isReply: Boolean = false,
    ) : ChatEvent
    data class Typing(val user: ChatUser, val isTyping: Boolean) : ChatEvent
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
