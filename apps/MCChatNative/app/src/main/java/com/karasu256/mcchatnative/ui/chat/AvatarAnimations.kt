package com.karasu256.mcchatnative.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.karasu256.mcchatnative.model.User
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private data class AvatarEntry(val user: User, val animateEnter: Boolean)

@Composable
fun AnimatedAvatarStack(
    users: List<User>,
    ringColor: Color,
    modifier: Modifier = Modifier,
    holdWhenEmpty: Boolean = false,
    enterMillis: Int = 250,
    exitMillis: Int = 200,
    overlap: Dp = 16.dp,
) {
    var roster by remember { mutableStateOf(users.map { AvatarEntry(it, animateEnter = false) }) }
    LaunchedEffect(users) {
        if (users.isEmpty()) {
            if (holdWhenEmpty) {
                delay(exitMillis.toLong().milliseconds)
                roster = emptyList()
            }
        } else {
            val known = roster.mapTo(HashSet()) { it.user.id }
            val animate = roster.isNotEmpty()
            roster = roster + users.filter { it.id !in known }
                .map { AvatarEntry(it, animateEnter = animate) }
        }
    }

    AvatarStack(modifier = modifier.animateContentSize(), overlap = overlap) {
        roster.forEach { entry ->
            key(entry.user.id) {
                AnimatedAvatar(
                    user = entry.user,
                    ringColor = ringColor,
                    visible = users.any { it.id == entry.user.id } || (holdWhenEmpty && users.isEmpty()),
                    animateEnter = entry.animateEnter,
                    enterMillis = enterMillis,
                    exitMillis = exitMillis,
                    onGone = { roster = roster.filter { it.user.id != entry.user.id } },
                )
            }
        }
    }
}

@Composable
private fun AnimatedAvatar(
    user: User,
    ringColor: Color,
    visible: Boolean,
    animateEnter: Boolean,
    enterMillis: Int,
    exitMillis: Int,
    onGone: () -> Unit,
) {
    val state = remember { MutableTransitionState(!animateEnter) }
    state.targetState = visible

    AnimatedVisibility(
        visibleState = state,
        enter = slideInHorizontally(tween(enterMillis)) { -it } + fadeIn(tween(enterMillis)),
        exit = scaleOut(tween(exitMillis), targetScale = 0.6f) + fadeOut(tween(exitMillis)),
    ) {
        UserAvatar(user, ringColor = ringColor)
    }

    LaunchedEffect(visible) {
        if (!visible) {
            delay(exitMillis.toLong().milliseconds)
            onGone()
        }
    }
}
