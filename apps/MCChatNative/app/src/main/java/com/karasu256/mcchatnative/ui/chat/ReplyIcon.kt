package com.karasu256.mcchatnative.ui.chat

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

val ReplyIcon: ImageVector by lazy {
    materialIcon(name = "Reply", autoMirror = true) {
        materialPath {
            moveTo(10f, 9f)
            verticalLineTo(5f)
            lineToRelative(-7f, 7f)
            lineToRelative(7f, 7f)
            verticalLineToRelative(-4.1f)
            curveToRelative(5f, 0f, 8.5f, 1.6f, 11f, 5.1f)
            curveToRelative(-1f, -5f, -4f, -10f, -11f, -11f)
            close()
        }
    }
}
