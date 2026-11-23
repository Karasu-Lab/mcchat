package com.karasu256.mcchatnative.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import com.karasu256.mcchatnative.R
import com.karasu256.mcchatnative.model.ChatUser

private val ListOrigin = TransformOrigin(1f, 0f)

@Composable
fun ActiveUsersAction(
    headerUsers: List<ChatUser>,
    allUsers: List<ChatUser>,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val description = stringResource(R.string.chat_active_users)
    val density = LocalDensity.current
    val positionProvider = remember(density) {
        BelowAnchorEndPositionProvider(
            gapPx = with(density) { 8.dp.roundToPx() },
            marginPx = with(density) { 8.dp.roundToPx() },
        )
    }

    Box(modifier = modifier.padding(end = 16.dp)) {
        AvatarStack(
            modifier = Modifier
                .clip(CircleShape)
                .clickable { expanded = true }
                .semantics { contentDescription = description },
        ) {
            headerUsers.forEach { user ->
                UserAvatar(user.name, ringColor = MaterialTheme.colorScheme.surface)
            }
        }
        ScalePopup(
            visible = expanded,
            positionProvider = positionProvider,
            transformOrigin = ListOrigin,
            onDismissRequest = { expanded = false },
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 6.dp,
            ) {
                LazyColumn(
                    modifier = Modifier
                        .width(220.dp)
                        .heightIn(max = 240.dp),
                ) {
                    items(allUsers, key = { it.id.value }) { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            UserAvatar(user.name)
                            Text(
                                text = user.name,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

private class BelowAnchorEndPositionProvider(
    private val gapPx: Int,
    private val marginPx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width - marginPx).coerceAtLeast(marginPx)
        val x = (anchorBounds.right - popupContentSize.width).coerceIn(marginPx, maxX)
        val y = anchorBounds.bottom + gapPx
        return IntOffset(x, y)
    }
}
