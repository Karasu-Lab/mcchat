package com.karasu256.mcchatnative.ui.chat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karasu256.mcchatnative.data.SkinCache
import com.karasu256.mcchatnative.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val AvatarSize = 32.dp

private fun faceOf(png: ByteArray): ImageBitmap? {
    val skin = BitmapFactory.decodeByteArray(png, 0, png.size) ?: return null
    val scale = skin.width / 64
    if (scale < 1) return null
    val face = Bitmap.createBitmap(8 * scale, 8 * scale, Bitmap.Config.ARGB_8888)
    val target = Rect(0, 0, face.width, face.height)
    val canvas = Canvas(face)
    canvas.drawBitmap(skin, Rect(8 * scale, 8 * scale, 16 * scale, 16 * scale), target, null)
    canvas.drawBitmap(skin, Rect(40 * scale, 8 * scale, 48 * scale, 16 * scale), target, null)
    return face.asImageBitmap()
}

@Composable
fun UserAvatar(
    user: User,
    ringColor: Color = Color.Transparent,
    size: Dp = AvatarSize,
) {
    val style = if (size < AvatarSize) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge
    val version by SkinCache.version.collectAsStateWithLifecycle()
    val face by produceState<ImageBitmap?>(null, user.id, version) {
        value = withContext(Dispatchers.Default) { SkinCache.get(user.id.value)?.let(::faceOf) }
    }
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(2.dp, ringColor),
        modifier = Modifier.size(size),
    ) {
        val bitmap = face
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = user.name,
                filterQuality = FilterQuality.None,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(user.name.take(1), style = style)
            }
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
