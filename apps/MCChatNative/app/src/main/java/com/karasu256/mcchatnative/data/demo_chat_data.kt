package com.karasu256.mcchatnative.data

import com.karasu256.mcchat.common.component.ChatText
import com.karasu256.mcchatnative.model.ChatEvent
import com.karasu256.mcchatnative.model.ChatMessage
import com.karasu256.mcchatnative.model.ReactionIntent
import com.karasu256.mcchatnative.model.Reactions
import com.karasu256.mcchatnative.model.User
import com.karasu256.mcchatnative.model.UserId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.transform

private fun demoUser(name: String) = User(UserId("demo-${name.lowercase()}"), name)

private val Alex = demoUser("Alex")
private val Sam = demoUser("Sam")
private val Mika = demoUser("Mika")
private val Ren = demoUser("Ren")

private val demoReactions = Reactions.generate()

private fun message(
    id: Long,
    sender: User,
    body: String,
    replyId: Long? = null,
): ChatEvent = ChatEvent.Message(id, sender, ChatText.raw(body), replyId)

private fun typing(user: User, isTyping: Boolean): ChatEvent =
    ChatEvent.Typing(user, isTyping)

private fun reaction(messageId: Long, reactionIndex: Int, user: User, isAdded: Boolean): ChatEvent =
    ChatEvent.Reaction(ReactionIntent(messageId, demoReactions[reactionIndex], user.id, isAdded))

private val demoSteps: List<List<ChatEvent>> = listOf(
    listOf(message(1, Alex, "ネザーに行く人いますか")),
    listOf(typing(Alex, true)),
    listOf(typing(Alex, false)),
    listOf(typing(Alex, true), typing(Sam, true)),
    listOf(typing(Sam, false)),
    listOf(message(2, Sam, "行きます、ダイヤのツルハシ持っていきます")),
    listOf(typing(Alex, false)),
    listOf(reaction(1, 0, Sam, true)),
    listOf(typing(Mika, true), typing(Ren, true)),
    listOf(typing(Mika, false)),
    listOf(message(3, Mika, "私も行く、松明多めに持っていくね")),
    listOf(reaction(2, 1, Mika, true), reaction(1, 0, Mika, true)),
    listOf(typing(Ren, false)),
    listOf(message(4, Ren, "拠点の前に集合しよう")),
    listOf(typing(Alex, true), typing(Sam, true), typing(Mika, true)),
    listOf(typing(Sam, false), typing(Mika, false)),
    listOf(message(5, Alex, "準備できたら出発します", replyId = 4)),
    listOf(typing(Alex, false)),
    listOf(reaction(4, 2, Alex, true), reaction(4, 2, Sam, true)),
    listOf(reaction(1, 0, Sam, false)),
    listOf(typing(Ren, true), typing(Mika, true)),
    listOf(message(6, Ren, "着いたら合図してください", replyId = 5)),
    listOf(typing(Mika, false)),
    listOf(reaction(6, 3, Mika, true), reaction(6, 3, Alex, true), reaction(6, 3, Sam, true)),
)

private val demoActiveUsers = listOf(
    Alex,
    Sam,
    Mika,
    Ren,
    demoUser("Yuki"),
    demoUser("Haru"),
    demoUser("Sora"),
    demoUser("Kai"),
    demoUser("Nao"),
    demoUser("Rin"),
)

object DemoChatEventSource : ChatEventSource {
    private val sent = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 8)
    private var nextReplyId = 100L

    override fun activeUsers(clientId: String): Flow<List<User>> = flowOf(demoActiveUsers)

    override suspend fun send(clientId: String, message: ChatMessage) {
        sent.emit(message)
    }

    override fun events(clientId: String): Flow<ChatEvent> {
        val stepMillis = 2_000L
        return merge(
            flow {
                for (event in demoSteps.flatten()) {
                    delay(stepMillis)
                    emit(event)
                }
            },
            sent.transform { message ->
                delay(stepMillis)
                emit(
                    message(
                        nextReplyId++,
                        Sam,
                        "了解です、一緒に行きましょう",
                        replyId = message.id,
                    )
                )
            },
        )
    }
}
