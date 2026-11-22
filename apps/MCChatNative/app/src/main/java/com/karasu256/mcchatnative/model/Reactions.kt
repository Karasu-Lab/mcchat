package com.karasu256.mcchatnative.model

data class ReactionData(val emoji: String)

data class ReactionIntent(
    val messageId: Long,
    val data: ReactionData,
    val userId: UserId,
    val isAdded: Boolean,
)

data class MessageReaction(
    val data: ReactionData,
    val userIds: List<UserId>,
) {
    val count: Int get() = userIds.size

    fun isReactedBy(userId: UserId): Boolean = userId in userIds
}

object Reactions {
    private val EmoticonCodePoints = 0x1F600..0x1F64F

    fun generate(): List<ReactionData> =
        EmoticonCodePoints.map { ReactionData(String(Character.toChars(it))) }
}
