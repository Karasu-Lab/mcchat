package com.karasu256.mcchatnative.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import com.karasu256.mcchat.common.PlayerRef
import com.karasu256.mcchat.common.component.ChatText
import com.karasu256.mcchat.common.component.MentionData
import com.karasu256.mcchatnative.model.User
import com.karasu256.mcchatnative.model.UserId
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
    completion: MentionData.Completion?,
    onInsert: (MentionData.Inserted) -> Unit,
    candidateLimit: Int = 5,
    anchorGap: Dp = 8.dp,
) {
    var shown by remember { mutableStateOf(completion) }
    SideEffect { if (completion != null) shown = completion }

    val gapPx = with(LocalDensity.current) { anchorGap.roundToPx() }
    val positionProvider = remember(gapPx) { AboveAnchorStartPositionProvider(gapPx) }

    ScalePopup(
        visible = completion != null,
        positionProvider = positionProvider,
        transformOrigin = TransformOrigin(0f, 1f),
        onDismissRequest = {},
        focusable = false,
    ) {
        shown?.let { current ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.large,
                shadowElevation = 6.dp,
            ) {
                Column {
                    current.candidates().take(candidateLimit).forEach { player ->
                        Row(
                            modifier = Modifier
                                .clickable { onInsert(current.insert(player)) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            UserAvatar(User(UserId(player.uuid().toString()), player.name()))
                            Text(text = player.name(), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}

private class AboveAnchorStartPositionProvider(private val gapPx: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset(
        anchorBounds.left,
        (anchorBounds.top - popupContentSize.height - gapPx).coerceAtLeast(0),
    )
}
