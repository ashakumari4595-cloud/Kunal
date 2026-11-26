package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ConversationSessionEntity
import com.example.ui.theme.AbyssBlack
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyberBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.MutedIce
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    conversations: List<ConversationSessionEntity>,
    activeSessionId: Long,
    onBack: () -> Unit,
    onNewChat: () -> Unit,
    onSelectConversation: (Long) -> Unit,
    onDeleteConversation: (Long) -> Unit,
    onClearAll: () -> Unit
) {
    var pendingDeleteId by remember { mutableStateOf<Long?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = AbyssBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.history_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("history_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back),
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (conversations.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearAllDialog = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("history_clear_all_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = stringResource(R.string.history_clear_all),
                                tint = CrimsonAlert
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightSurface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewChat,
                containerColor = CyberBlue,
                contentColor = Color.White,
                modifier = Modifier.testTag("history_new_chat_fab")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.nav_new_chat)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AbyssBlack)
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            if (conversations.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChatBubbleOutline,
                        contentDescription = null,
                        tint = ElectricCyan.copy(alpha = 0.7f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.history_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.history_empty_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedIce
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onNewChat,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberBlue)
                    ) {
                        Text(stringResource(R.string.nav_new_chat))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(conversations, key = { it.id }) { session ->
                        val isActive = session.id == activeSessionId
                        ConversationHistoryCard(
                            session = session,
                            isActive = isActive,
                            onClick = { onSelectConversation(session.id) },
                            onDelete = { pendingDeleteId = session.id }
                        )
                    }
                }
            }
        }
    }

    pendingDeleteId?.let { targetId ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            containerColor = ElevatedSlate,
            titleContentColor = Color.White,
            textContentColor = MutedIce,
            title = { Text("Delete Conversation?") },
            text = { Text("This saved conversation and its messages will be permanently removed from this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteConversation(targetId)
                        pendingDeleteId = null
                    }
                ) {
                    Text("Delete", color = CrimsonAlert)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            containerColor = ElevatedSlate,
            titleContentColor = Color.White,
            textContentColor = MutedIce,
            title = { Text("Clear All History?") },
            text = { Text("All saved conversations on this device will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAll()
                        showClearAllDialog = false
                    }
                ) {
                    Text("Clear All", color = CrimsonAlert)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun ConversationHistoryCard(
    session: ConversationSessionEntity,
    isActive: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val formattedDate = remember(session.updatedAtMillis) {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            .format(Date(session.updatedAtMillis))
    }

    Surface(
        color = if (isActive) ElevatedSlate else MidnightSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isActive) ElectricCyan else HairlineBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("history_item_${session.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (session.lastMessagePreview.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = session.lastMessagePreview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedIce,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan.copy(alpha = 0.85f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("delete_chat_${session.id}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = stringResource(R.string.history_delete_chat),
                    tint = CrimsonAlert
                )
            }
        }
    }
}
