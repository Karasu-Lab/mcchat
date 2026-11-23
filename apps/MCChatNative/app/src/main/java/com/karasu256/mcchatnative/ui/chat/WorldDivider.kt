package com.karasu256.mcchatnative.ui.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karasu256.mcchatnative.R
import com.karasu256.mcchatnative.data.WorldRegistry
import com.karasu256.mcchatnative.model.World

@Composable
fun WorldDivider(world: World, modifier: Modifier = Modifier) {
    val registry by WorldRegistry.worlds.collectAsStateWithLifecycle()
    val worldName = (registry[world.id] ?: world).name
    val label = if (worldName.isBlank()) {
        stringResource(R.string.chat_world_changed)
    } else {
        stringResource(R.string.chat_world_entered, worldName)
    }

    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        AnimatedContent(
            targetState = label,
            transitionSpec = { fadeIn() togetherWith fadeOut() using SizeTransform() },
        ) { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}
