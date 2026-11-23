package com.karasu256.mcchatnative.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle

private const val EnterMillis = 400
private const val ExitMillis = 200
private const val ParallaxDivisor = 4

object PageTransitions : NavHostAnimatedDestinationStyle() {
    override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        {
            slideInHorizontally(tween(EnterMillis)) { it } + fadeIn(tween(EnterMillis))
        }

    override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        {
            slideOutHorizontally(tween(EnterMillis)) { -it / ParallaxDivisor } + fadeOut(
                tween(
                    ExitMillis
                )
            )
        }

    override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        {
            slideInHorizontally(tween(EnterMillis)) { -it / ParallaxDivisor } + fadeIn(
                tween(
                    EnterMillis
                )
            )
        }

    override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        {
            slideOutHorizontally(tween(EnterMillis)) { it } + fadeOut(tween(ExitMillis))
        }
}
