package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.viewmodel.ChatMessage
import com.example.viewmodel.FileManagerViewModel
import com.example.GlobalProfileAvatarButton
import kotlinx.coroutines.launch

@Composable
fun AIScreen(
    viewModel: FileManagerViewModel,
    onMenuClick: () -> Unit = {}
) {
    val messages by viewModel.chatbotMessages.collectAsStateWithLifecycle()
    val customKey by viewModel.geminiApiKey.collectAsStateWithLifecycle()
    val useHighThinking by viewModel.useHighThinking.collectAsStateWithLifecycle()
    val isSending by viewModel.isSendingToGemini.collectAsStateWithLifecycle()
    val isPanelExpanded by viewModel.liveSetupPanelExpanded.collectAsStateWithLifecycle()

    var inputPrompt by remember { mutableStateOf("") }
    var hideKeyText by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Slide-down focus scroll whenever messages grow
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 8.dp) // Bottom padding
    ) {
        // Screen Header Banners
        AIHeaderBanner(viewModel = viewModel, onMenuClick = onMenuClick)

        // 1. Collapsible Gemini Setup Panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Collapsible Trigger Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickableNoRipple { viewModel.liveSetupPanelExpanded.value = !isPanelExpanded }
                        .testTag("setup_panel_collapsible_header"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            tint = CustomFlameOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gemini AI Engine Setup & Panel",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    IconButton(
                        onClick = { viewModel.liveSetupPanelExpanded.value = !isPanelExpanded },
                        modifier = Modifier.testTag("btn_toggle_setup_panel")
                    ) {
                        Icon(
                            imageVector = if (isPanelExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle setup configuration details",
                            tint = Color.White
                        )
                    }
                }

                // Expanded settings contents
                AnimatedVisibility(
                    visible = isPanelExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Text(
                            text = "Provide a custom runtime Google Gemini API Key to bypass platform bounds, or use the pre-configured default sandbox key.",
                            fontSize = 11.sp,
                            color = TextGray,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Custom Key input field
                        OutlinedTextField(
                            value = customKey,
                            onValueChange = { viewModel.geminiApiKey.value = it },
                            label = { Text("Custom Gemini API Key (Optional)") },
                            singleLine = true,
                            visualTransformation = if (hideKeyText) PasswordVisualTransformation() else VisualTransformation.None,
                            trailingIcon = {
                                IconButton(onClick = { hideKeyText = !hideKeyText }) {
                                    Icon(
                                        imageVector = if (hideKeyText) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Key Visibility"
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CustomFlameOrange,
                                unfocusedBorderColor = Color.Gray
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_api_key_field")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Mind Level selector (High-Thinking option)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Psychology,
                                        contentDescription = null,
                                        tint = AquaticWaveBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "High-Thinking Optimizer",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = "Forces model to think logically in a scratchpad before speaking.",
                                    fontSize = 10.sp,
                                    color = TextGray,
                                    lineHeight = 13.sp
                                )
                            }

                            Switch(
                                checked = useHighThinking,
                                onCheckedChange = { viewModel.useHighThinking.value = it },
                                modifier = Modifier.testTag("high_thinking_toggle_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CustomFlameOrange,
                                    checkedTrackColor = CustomFlameOrange.copy(alpha = 0.4f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Active Indicator status box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (useHighThinking) CustomFlameOrange.copy(alpha = 0.1f) else CosmicPrimary.copy(alpha = 0.2f)
                                )
                                .padding(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (useHighThinking) Icons.Default.Bolt else Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = if (useHighThinking) CustomFlameOrange else AquaticWaveBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (useHighThinking) {
                                        "Active Engine: models/gemini-1.5-pro (ThinkingLevel: HIGH)"
                                    } else {
                                        "Active Engine: models/gemini-2.0-flash (Rapid Velocity Mode)"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (useHighThinking) CustomFlameOrange else AquaticWaveBlue
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Chat Log messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                ChatBubble(message = message)
            }

            if (isSending) {
                item {
                    // Pulsing/loading state representation for AI reply
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(CustomFlameOrange.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = CustomFlameOrange,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gemini is thinking logically inside active scratchpad...",
                            fontSize = 11.sp,
                            color = CustomFlameOrange,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. User typing input panel & Clean History Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Prune conversation history action
            IconButton(
                onClick = { viewModel.clearChatHistory() },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .size(48.dp)
                    .testTag("clear_chat_history_btn")
            ) {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = "Clear conversation history",
                    tint = MaterialTheme.colorScheme.error
                )
            }

            // Text Area Input
            OutlinedTextField(
                value = inputPrompt,
                onValueChange = { inputPrompt = it },
                placeholder = { Text("Ask about folders, space mathematical, etc...", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_text_input_field"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CustomFlameOrange,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            // Submit prompt button
            IconButton(
                onClick = {
                    val query = inputPrompt.trim()
                    if (query.isNotBlank() && !isSending) {
                        viewModel.sendChatMessage(query)
                        inputPrompt = ""
                    }
                },
                enabled = inputPrompt.isNotBlank() && !isSending,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (inputPrompt.isNotBlank() && !isSending) CustomFlameOrange else Color.Gray.copy(alpha = 0.3f)
                    )
                    .size(48.dp)
                    .testTag("send_chat_btn")
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Transmit prompt",
                    tint = Color.White
                )
            }
        }
    }
}

// --- Screen Header Banner ---
@Composable
fun AIHeaderBanner(
    viewModel: FileManagerViewModel,
    onMenuClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        CosmicPrimary.copy(alpha = 0.95f),
                        CharcoalDarkBg
                    )
                )
            )
            .padding(top = 16.dp, start = 12.dp, end = 16.dp, bottom = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.testTag("btn_open_menu")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open Navigation Drawer",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CustomFlameOrange.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = CustomFlameOrange,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "AI SPACE COMMAND",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    ),
                    color = Color.White
                )
                Text(
                    text = "Conversational Storage Optimization",
                    style = MaterialTheme.typography.bodySmall,
                    color = CustomFlameOrange
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            GlobalProfileAvatarButton(viewModel = viewModel)
        }
    }
}

// --- Chat Bubble bubble ---
@Composable
fun ChatBubble(message: ChatMessage) {
    val alignEnd = message.isUser

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            if (!alignEnd) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(CustomFlameOrange.copy(alpha = 0.15f))
                        .align(Alignment.Top),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = CustomFlameOrange,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // The Message Box
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (alignEnd) 16.dp else 2.dp,
                            bottomEnd = if (alignEnd) 2.dp else 16.dp
                        )
                    )
                    .background(
                        if (alignEnd) {
                            CosmicPrimary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        }
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = message.text,
                    fontSize = 13.sp,
                    color = Color.White,
                    lineHeight = 18.sp
                )
            }

            if (alignEnd) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(CosmicPrimary)
                        .align(Alignment.Top),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = CustomFlameOrange,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// Simple helper to avoid ripple sound bugs in list elements
@Composable
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    return this.clickable(
        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}
