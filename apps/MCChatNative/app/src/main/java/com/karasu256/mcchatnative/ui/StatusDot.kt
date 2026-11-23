package com.karasu256.mcchatnative.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StatusDot(isOnline: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(8.dp)
            .background(
                color = if (isOnline) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape,
            ),
    )
}
