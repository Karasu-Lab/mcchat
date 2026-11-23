package com.karasu256.mcchatnative.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.karasu256.mcchatnative.R
import com.karasu256.mcchatnative.model.User

@Composable
fun ActiveUsersAction(
    headerUsers: List<User>,
    allUsers: List<User>,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val description = stringResource(R.string.chat_active_users)

    Box(modifier = modifier) {
        AvatarStack(
            modifier = Modifier
                .clip(CircleShape)
                .clickable { expanded = true }
                .semantics { contentDescription = description },
        ) {
            headerUsers.forEach { user ->
                UserAvatar(user, ringColor = MaterialTheme.colorScheme.surface)
            }
        }
        HeaderPopup(visible = expanded, onDismissRequest = { expanded = false }) {
            LazyColumn(
                modifier = Modifier
                    .width(220.dp)
                    .heightIn(max = 240.dp),
            ) {
                items(allUsers, key = { it.id.value }) { user ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        UserAvatar(user)
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
