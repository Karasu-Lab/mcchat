package com.karasu256.mcchatnative.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

private const val PopupEnterMillis = 200
private const val PopupExitMillis = 150
private const val PopupInitialScale = 0.6f

@Composable
fun ScalePopup(
    visible: Boolean,
    positionProvider: PopupPositionProvider,
    transformOrigin: TransformOrigin,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val state = remember { MutableTransitionState(false) }
    state.targetState = visible

    if (!state.currentState && !state.targetState) return

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        AnimatedVisibility(
            visibleState = state,
            modifier = modifier,
            enter = fadeIn(tween(PopupEnterMillis)) +
                    scaleIn(
                        tween(PopupEnterMillis),
                        initialScale = PopupInitialScale,
                        transformOrigin = transformOrigin
                    ),
            exit = fadeOut(tween(PopupExitMillis)) +
                    scaleOut(
                        tween(PopupExitMillis),
                        targetScale = PopupInitialScale,
                        transformOrigin = transformOrigin
                    ),
        ) {
            content()
        }
    }
}
