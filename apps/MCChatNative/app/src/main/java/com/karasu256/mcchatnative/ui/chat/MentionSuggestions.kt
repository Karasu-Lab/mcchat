package com.karasu256.mcchatnative.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.karasu256.mcchat.common.PlayerRef
import com.karasu256.mcchat.common.component.ChatText
import com.karasu256.mcchat.common.component.MentionData
import com.karasu256.mcchatnative.model.User
import com.karasu256.mcchatnative.model.uuid
import java.util.Optional

fun List<User>.toPlayerRefs(): List<PlayerRef> = mapNotNull { user ->
    user.id.uuid()?.let { PlayerRef(it, user.name, Optional.empty()) }
}

fun ChatText.annotated(mentionColor: Color): AnnotatedString = buildAnnotatedString {
    components().forEach { component ->
        if (component is MentionData) {
            withStyle(SpanStyle(color = mentionColor, fontWeight = FontWeight.Bold)) {
                append(component.plain())
            }
        } else {
            append(component.plain())
        }
    }
}

class MentionVisualTransformation(
    private val players: List<PlayerRef>,
    private val mentionColor: Color,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(
            ChatText.parse(text.text, players).annotated(mentionColor),
            OffsetMapping.Identity,
        )
}

@Composable
fun MentionSuggestions(
    completion: MentionData.Completion,
    onInsert: (MentionData.Inserted) -> Unit,
    modifier: Modifier = Modifier,
    candidateLimit: Int = 5,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
        modifier = modifier,
    ) {
        Column {
            completion.candidates().take(candidateLimit).forEach { player ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onInsert(completion.insert(player)) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    UserAvatar(User(com.karasu256.mcchatnative.model.UserId(player.uuid().toString()), player.name()))
                    Text(text = player.name(), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
