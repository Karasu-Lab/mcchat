package com.karasu256.mcchatnative.ui.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SwipeToReply(
    onReply: () -> Unit,
    modifier: Modifier = Modifier,
    threshold: Dp = 64.dp,
    maxDrag: Dp = 96.dp,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val currentOnReply by rememberUpdatedState(onReply)
    val density = LocalDensity.current
    val thresholdPx = with(density) { threshold.toPx() }
    val maxDragPx = with(density) { maxDrag.toPx() }

    Box(
        modifier = modifier.pointerInput(thresholdPx, maxDragPx, enabled) {
            if (!enabled) return@pointerInput
            detectHorizontalDragGestures(
                onDragEnd = {
                    if (offset.value <= -thresholdPx) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentOnReply()
                    }
                    scope.launch { offset.animateTo(0f) }
                },
                onDragCancel = { scope.launch { offset.animateTo(0f) } },
            ) { change, dragAmount ->
                change.consume()
                scope.launch { offset.snapTo((offset.value + dragAmount).coerceIn(-maxDragPx, 0f)) }
            }
        },
    ) {
        Icon(
            imageVector = ReplyIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
                .graphicsLayer { alpha = (-offset.value / thresholdPx).coerceIn(0f, 1f) },
        )
        Box(modifier = Modifier.offset { IntOffset(offset.value.roundToInt(), 0) }) {
            content()
        }
    }
}
