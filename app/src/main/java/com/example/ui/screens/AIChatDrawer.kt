package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.ChatMessage
import com.example.viewmodel.FileManagerViewModel
import com.example.ui.theme.CustomFlameOrange
import com.example.ui.theme.DeepSurfaceDark
import com.example.ui.theme.TextGray

@Composable
fun AIChatDrawer(
    viewModel: FileManagerViewModel,
    onClose: () -> Unit
) {
    val chatDrawerMessages by viewModel.chatDrawerMessages.collectAsStateWithLifecycle()
    val isSendingDrawerToGemini by viewModel.isSendingDrawerToGemini.collectAsStateWithLifecycle()
    var userText by remember { mutableStateOf("") }
    
    Card(
        shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
        colors = CardDefaults.cardColors(containerColor = DeepSurfaceDark),
        modifier = Modifier
            .fillMaxHeight()
            .width(340.dp) // Professional side-panel width
            .testTag("ai_chat_drawer")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = CustomFlameOrange,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Vishwa Client AI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Natural file auditor",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    }
                }
                
                Row {
                    IconButton(onClick = { viewModel.clearDrawerChatHistory() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset Conversation", tint = TextGray)
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close Drawer", tint = Color.White)
                    }
                }
            }
            
            HorizontalDivider(
                color = Color.White.copy(alpha = 0.08f), 
                modifier = Modifier.padding(vertical = 12.dp)
            )
            
            // Helpful chips to click and immediately ask questions about files
            Text(
                text = "TAP QUICK ASSIST PROBLEMS:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = CustomFlameOrange,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "📊 Breakdown" to "Can you list my local files and give me a full storage breakdown by category, calculating the percentage of space used?",
                    "⚖️ Duplicates" to "Are there duplicate files on my device based on size pairings? List them.",
                    "🗑️ Junk stats" to "How many junk files are there on my device and what is the total size we can reclaim?"
                ).forEach { (label, prompt) ->
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .clickable { viewModel.sendDrawerChatMessage(prompt) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = label, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            
            // Conversation Column
            val lazyListState = rememberLazyListState()
            LaunchedEffect(chatDrawerMessages.size) {
                if (chatDrawerMessages.isNotEmpty()) {
                    lazyListState.animateScrollToItem(chatDrawerMessages.size - 1)
                }
            }
            
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(chatDrawerMessages) { message ->
                    DrawerChatBubble(message = message)
                }
                if (isSendingDrawerToGemini) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            CircularProgressIndicator(
                                color = CustomFlameOrange,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "AI is reviewing file registries...",
                                fontSize = 11.sp,
                                color = TextGray
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Text Entry Input Box
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = userText,
                    onValueChange = { userText = it },
                    placeholder = { Text("Ask about files...", fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CustomFlameOrange,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.02f),
                        focusedContainerColor = Color.White.copy(alpha = 0.02f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (userText.isNotBlank()) {
                            viewModel.sendDrawerChatMessage(userText)
                            userText = ""
                        }
                    },
                    modifier = Modifier
                        .background(CustomFlameOrange, CircleShape)
                        .size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DrawerChatBubble(message: ChatMessage) {
    val bubbleColor = if (message.isUser) CustomFlameOrange else Color.White.copy(alpha = 0.05f)
    val align = if (message.isUser) Alignment.End else Alignment.Start
    val shape = if (message.isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp)
    }
    
    Column(
        horizontalAlignment = align,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(bubbleColor, shape)
                .padding(12.dp)
                .widthIn(max = 240.dp)
        ) {
            Text(
                text = message.text,
                fontSize = 13.sp,
                color = Color.White,
                lineHeight = 18.sp
            )
        }
    }
}
