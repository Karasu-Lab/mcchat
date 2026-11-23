package com.karasu256.mcchatnative.ui.chat

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupPositionProvider
import com.karasu256.mcchatnative.model.ReactionData
import kotlinx.coroutines.delay

@Composable
fun ReactionPicker(
    visible: Boolean,
    reactions: List<ReactionData>,
    selected: Set<ReactionData>,
    onReact: (ReactionData) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    transformOrigin: TransformOrigin = TransformOrigin(0.5f, 1f),
) {
    val density = LocalDensity.current
    val positionProvider = remember(density) {
        AboveAnchorPositionProvider(
            gapPx = with(density) { 8.dp.roundToPx() },
            marginPx = with(density) { 8.dp.roundToPx() },
        )
    }

    ScalePopup(
        visible = visible,
        positionProvider = positionProvider,
        transformOrigin = transformOrigin,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                reactions.forEachIndexed { index, reaction ->
                    ReactionItem(
                        reaction = reaction,
                        index = index,
                        selected = reaction in selected,
                        onClick = { onReact(reaction) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReactionItem(
    reaction: ReactionData,
    index: Int,
    selected: Boolean,
    onClick: () -> Unit,
    enterMillis: Int = 200,
    staggerMillis: Long = 40L,
    initialScale: Float = 0.4f,
) {
    val selectedColor = MaterialTheme.colorScheme.secondaryContainer
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * staggerMillis)
        shown = true
    }
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(enterMillis),
        label = "reactionItem",
    )

    Box(
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer {
                val scale = initialScale + (1f - initialScale) * progress
                scaleX = scale
                scaleY = scale
                alpha = progress
            }
            .clip(CircleShape)
            .background(if (selected) selectedColor else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = reaction.emoji, fontSize = 24.sp)
    }
}

private class AboveAnchorPositionProvider(
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
        val x = (anchorBounds.center.x - popupContentSize.width / 2).coerceIn(marginPx, maxX)
        val above = anchorBounds.top - popupContentSize.height - gapPx
        val y = if (above >= marginPx) above else anchorBounds.bottom + gapPx
        return IntOffset(x, y)
    }
}
