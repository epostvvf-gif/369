package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import com.example.viewmodel.FileManagerViewModel
import com.example.viewmodel.JunkItem
import com.example.GlobalProfileAvatarButton

@Composable
fun JunkCleanerScreen(
    viewModel: FileManagerViewModel,
    onMenuClick: () -> Unit
) {
    val isJunkScanning by viewModel.isJunkScanning.collectAsStateWithLifecycle()
    val isJunkCleaning by viewModel.isJunkCleaning.collectAsStateWithLifecycle()
    val scannedJunkItems by viewModel.scannedJunkItems.collectAsStateWithLifecycle()
    val junkBytesCleaned by viewModel.junkBytesCleaned.collectAsStateWithLifecycle()
    val showCelebrationDialog by viewModel.showCelebrationDialog.collectAsStateWithLifecycle()
    val isGeminiJunkScanning by viewModel.isGeminiJunkScanning.collectAsStateWithLifecycle()
    val aiSuggestedJunkItems by viewModel.aiSuggestedJunkItems.collectAsStateWithLifecycle()

    // Auto trigger scan on initial load if empty to make it engaging
    LaunchedEffect(Unit) {
        if (scannedJunkItems.isEmpty()) {
            viewModel.startJunkScan()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalDarkBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Screen Header Banner with Navigation Hamburger Menu
            JunkHeaderBanner(viewModel = viewModel, onMenuClick = onMenuClick)

            if (isJunkScanning) {
                // Scanning view with pulsing circle animation & details
                JunkScanningProgressState()
            } else if (isJunkCleaning) {
                // Cleaning execution in progress
                JunkCleaningInProgressState()
            } else {
                // Interactive Scanner Results checklists & CTA trigger
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    var activeSubTab by remember { mutableStateOf(0) } // 0: Standard, 1: Gemini AI

                    val checkedItems = scannedJunkItems.filter { it.isChecked }
                    val totalReclaimableSize = checkedItems.sumOf { it.size }
                    val cacheItems = scannedJunkItems.filter { !it.isFolder }
                    val emptyFolders = scannedJunkItems.filter { it.isFolder }

                    val aiCheckedItems = aiSuggestedJunkItems.filter { it.isChecked }
                    val totalAiReclaimableSize = aiCheckedItems.sumOf { it.size }
                    
                    // Filter duplicate items vs. large files based on names/reasons
                    val aiDuplicateItems = aiSuggestedJunkItems.filter { 
                        it.name.contains("Copy", ignoreCase = true) || 
                        it.name.contains("backup", ignoreCase = true) || 
                        it.name.contains("_dup", ignoreCase = true) || 
                        it.aiReason?.contains("duplicate", ignoreCase = true) == true 
                    }
                    val aiLargeItems = aiSuggestedJunkItems.filter { 
                        !aiDuplicateItems.contains(it) 
                    }

                    val currentReclaimSize = if (activeSubTab == 0) totalReclaimableSize else totalAiReclaimableSize

                    // Brief explanation
                    Text(
                        text = "Vishwa Space Optimizer results. Selected entries can be securely deleted to restore storage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Tab Selector Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (activeSubTab == 0) CustomFlameOrange else Color.Transparent)
                                .clickable { activeSubTab = 0 }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Standard Clean",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeSubTab == 0) Color.White else TextGray
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (activeSubTab == 1) CustomFlameOrange else Color.Transparent)
                                .clickable { 
                                    activeSubTab = 1 
                                    if (aiSuggestedJunkItems.isEmpty() && !isGeminiJunkScanning) {
                                        viewModel.startGeminiJunkScan()
                                    }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (activeSubTab == 1) Color.White else CustomFlameOrange,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Gemini AI Smart Check",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeSubTab == 1) Color.White else TextGray
                                )
                            }
                        }
                    }

                    // Reclaim Metrics Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = DeepSurfaceDark),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (activeSubTab == 0) "Ready to Reclaim" else "Gemini AI Suggested Space",
                                    fontSize = 12.sp,
                                    color = TextGray,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = viewModel.formatFileSize(currentReclaimSize),
                                    fontSize = 24.sp,
                                    color = CustomFlameOrange,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(CustomFlameOrange.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (activeSubTab == 0) Icons.Default.BatterySaver else Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = CustomFlameOrange,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // Checklist List
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (activeSubTab == 0) {
                            // Standard Clean Lists
                            if (cacheItems.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "CACHE & TEMPORARY FILES (${cacheItems.size})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = CustomFlameOrange,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                                items(cacheItems) { item ->
                                    JunkCleanerItemRow(
                                        item = item,
                                        onToggle = { viewModel.toggleJunkItem(item.id) },
                                        viewModel = viewModel
                                    )
                                }
                            }

                            if (emptyFolders.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "EMPTY SYSTEM FOLDER ENTRIES (${emptyFolders.size})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = AquaticWaveBlue,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                                items(emptyFolders) { item ->
                                    JunkCleanerItemRow(
                                        item = item,
                                        onToggle = { viewModel.toggleJunkItem(item.id) },
                                        viewModel = viewModel
                                    )
                                }
                            }

                            if (scannedJunkItems.isEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 48.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = ForestEcoGreen,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Your storage is fully optimized!",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "No residual junk folders or cache files detected.",
                                            fontSize = 12.sp,
                                            color = TextGray,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = { viewModel.startJunkScan() },
                                            colors = ButtonDefaults.buttonColors(containerColor = DynamicDarkM3PillColor),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Scan Again", fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        } else {
                            // Gemini AI Smart Clean Lists
                            if (isGeminiJunkScanning) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 48.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator(color = CustomFlameOrange)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Gemini AI indexing-engine scanning file metadata...",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Comparing identical sizes and file naming structures",
                                            fontSize = 11.sp,
                                            color = TextGray,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                if (aiDuplicateItems.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "GEMINI DETECTED DUPLICATE COPIES (${aiDuplicateItems.size})",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = CustomFlameOrange,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                    items(aiDuplicateItems) { item ->
                                        JunkCleanerItemRow(
                                            item = item,
                                            onToggle = { viewModel.toggleGeminiJunkItem(item.id) },
                                            viewModel = viewModel
                                        )
                                    }
                                }

                                if (aiLargeItems.isNotEmpty()) {
                                    item {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "GEMINI CRITICAL LARGE FILES WARNING (>10MB) (${aiLargeItems.size})",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = CustomFlameOrange,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                    items(aiLargeItems) { item ->
                                        JunkCleanerItemRow(
                                            item = item,
                                            onToggle = { viewModel.toggleGeminiJunkItem(item.id) },
                                            viewModel = viewModel
                                        )
                                    }
                                }

                                if (aiSuggestedJunkItems.isEmpty()) {
                                    item {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 48.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                Icons.Default.Verified,
                                                contentDescription = null,
                                                tint = ForestEcoGreen,
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "AI Metadata Analysis Clean!",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Gemini checked all files and found no duplicated replicas or redundant large logs.",
                                                fontSize = 12.sp,
                                                color = TextGray,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Button(
                                                onClick = { viewModel.startGeminiJunkScan() },
                                                colors = ButtonDefaults.buttonColors(containerColor = DynamicDarkM3PillColor),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text("Analyze Again", fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Sticky Action Buttons Bottom Row
                    if (activeSubTab == 0 && scannedJunkItems.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.cleanSelectedJunk() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("btn_clean_selected_junk"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CustomFlameOrange),
                                border = BorderStroke(1.5.dp, CustomFlameOrange.copy(alpha = 0.8f)),
                                shape = RoundedCornerShape(12.dp),
                                enabled = checkedItems.isNotEmpty()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Clean Selected",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Button(
                                onClick = { viewModel.cleanAllJunk() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("btn_clean_all_junk"),
                                colors = ButtonDefaults.buttonColors(containerColor = CustomFlameOrange),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Clear All",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    } else if (activeSubTab == 1 && aiSuggestedJunkItems.isNotEmpty() && !isGeminiJunkScanning) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.startGeminiJunkScan() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("btn_re_scan_ai_junk"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextGray),
                                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "AI Review",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Button(
                                onClick = { viewModel.cleanSelectedGeminiJunk() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("btn_purge_selected_ai_junk"),
                                colors = ButtonDefaults.buttonColors(containerColor = CustomFlameOrange),
                                shape = RoundedCornerShape(12.dp),
                                enabled = aiCheckedItems.isNotEmpty()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Purge Suggested",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Victory celebration banner/dialog is handled inside MainActivity or custom overlay, but let's add the Dialog here
        if (showCelebrationDialog) {
            Dialog(onDismissRequest = { viewModel.showCelebrationDialog.value = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("celebration_clean_dialog"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepSurfaceDark),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(ForestEcoGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.DoneAll,
                                contentDescription = null,
                                tint = ForestEcoGreen,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Clean Completed!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "You successfully removed duplicate caches and empty database categories, reclaiming:",
                            fontSize = 12.sp,
                            color = TextGray,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = viewModel.formatFileSize(junkBytesCleaned),
                            fontSize = 28.sp,
                            color = ForestEcoGreen,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.showCelebrationDialog.value = false },
                            colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Awesome", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

val DynamicDarkM3PillColor = Color(0xFF1E2E52)

@Composable
fun JunkHeaderBanner(
    viewModel: FileManagerViewModel,
    onMenuClick: () -> Unit
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
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.testTag("btn_open_menu")
            ) {
                Icon(Icons.Default.Menu, contentDescription = "Open Navigation Drawer", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CustomFlameOrange.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = CustomFlameOrange,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "VISHVA CLEANER",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    ),
                    color = Color.White
                )
                Text(
                    text = "System Care & Caches",
                    style = MaterialTheme.typography.bodySmall,
                    color = CustomFlameOrange
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            GlobalProfileAvatarButton(viewModel = viewModel)
        }
    }
}

@Composable
fun JunkScanningProgressState() {
    val infiniteTransition = rememberInfiniteTransition(label = "scanning_pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha_pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(140.dp)
        ) {
            CircularProgressIndicator(
                color = CustomFlameOrange,
                strokeWidth = 6.dp,
                modifier = Modifier.size(96.dp)
            )
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(CustomFlameOrange.copy(alpha = alphaAnim * 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Memory,
                    contentDescription = null,
                    tint = CustomFlameOrange,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Scanning Storage Sectors",
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Analyzing temporary directories & duplicate configurations...",
            fontSize = 12.sp,
            color = TextGray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun JunkCleaningInProgressState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = ForestEcoGreen,
            strokeWidth = 6.dp,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Purging Checked Junk Files...",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Physically recovering cache directories. Do not close the space program.",
            fontSize = 12.sp,
            color = TextGray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun JunkCleanerItemRow(
    item: JunkItem,
    onToggle: () -> Unit,
    viewModel: FileManagerViewModel
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector = if (item.isFolder) Icons.Default.FolderOpen else Icons.Default.InsertDriveFile,
            contentDescription = null,
            tint = if (item.isFolder) AquaticWaveBlue else TextGray,
            modifier = Modifier.size(28.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.path,
                fontSize = 11.sp,
                color = TextGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.isAiSuggested && !item.aiReason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(CustomFlameOrange.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Reason",
                        tint = CustomFlameOrange,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.aiReason,
                        fontSize = 9.sp,
                        color = CustomFlameOrange,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = viewModel.formatFileSize(item.size),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = CustomFlameOrange
            )
        }

        Checkbox(
            checked = item.isChecked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = CustomFlameOrange,
                uncheckedColor = TextGray
            )
        )
    }
}
