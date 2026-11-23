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

@Composable
fun ScalePopup(
    visible: Boolean,
    positionProvider: PopupPositionProvider,
    transformOrigin: TransformOrigin,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    enterMillis: Int = 200,
    exitMillis: Int = 150,
    initialScale: Float = 0.6f,
    focusable: Boolean = true,
    content: @Composable () -> Unit,
) {
    val state = remember { MutableTransitionState(false) }
    state.targetState = visible

    if (!state.currentState && !state.targetState) return

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = focusable),
    ) {
        AnimatedVisibility(
            visibleState = state,
            modifier = modifier,
            enter = fadeIn(tween(enterMillis)) +
                    scaleIn(
                        tween(enterMillis),
                        initialScale = initialScale,
                        transformOrigin = transformOrigin
                    ),
            exit = fadeOut(tween(exitMillis)) +
                    scaleOut(
                        tween(exitMillis),
                        targetScale = initialScale,
                        transformOrigin = transformOrigin
                    ),
        ) {
            content()
        }
    }
}
