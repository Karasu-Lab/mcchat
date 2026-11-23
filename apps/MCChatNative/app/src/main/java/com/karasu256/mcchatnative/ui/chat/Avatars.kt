package com.karasu256.mcchatnative.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val AvatarSize = 32.dp

@Composable
fun UserAvatar(
    name: String,
    ringColor: Color = Color.Transparent,
    size: Dp = AvatarSize,
) {
    val style = if (size < AvatarSize) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(2.dp, ringColor),
        modifier = Modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(name.take(1), style = style)
        }
    }
}

@Composable
fun AvatarStack(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(-AvatarSize / 2),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
