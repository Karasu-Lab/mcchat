package com.karasu256.mcchatnative.ui.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.karasu256.mcchatnative.R
import com.karasu256.mcchatnative.ui.theme.SplashInk
import com.karasu256.mcchatnative.ui.theme.SplashPink
import com.karasu256.mcchatnative.ui.theme.SplashSky

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    gradientSpan: Float = 900f,
    cycleMillis: Int = 4000,
) {
    val transition = rememberInfiniteTransition(label = "splash")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = gradientSpan * 2,
        animationSpec = infiniteRepeatable(
            tween(cycleMillis, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "shift",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    Brush.linearGradient(
                        colors = listOf(SplashPink, SplashSky),
                        start = Offset(shift, shift),
                        end = Offset(shift + gradientSpan, shift + gradientSpan),
                        tileMode = TileMode.Mirror,
                    ),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = SplashInk,
            )
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = SplashInk,
                strokeWidth = 3.dp,
            )
        }
    }
}
