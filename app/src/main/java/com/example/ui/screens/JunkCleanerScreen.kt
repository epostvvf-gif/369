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
                    val checkedItems = scannedJunkItems.filter { it.isChecked }
                    val totalReclaimableSize = checkedItems.sumOf { it.size }
                    val cacheItems = scannedJunkItems.filter { !it.isFolder }
                    val emptyFolders = scannedJunkItems.filter { it.isFolder }

                    // Brief explanation
                    Text(
                        text = "Vishwa Space Optimizer results. Selected entries can be securely deleted to restore storage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

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
                                    text = "Ready to Reclaim",
                                    fontSize = 12.sp,
                                    color = TextGray,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = viewModel.formatFileSize(totalReclaimableSize),
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
                                    Icons.Default.BatterySaver,
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
                                    text = "EMPTY SYSTEM FOLDERS (${emptyFolders.size})",
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
                    }

                    // Primary Clean Trigger Sticky Button Row
                    if (scannedJunkItems.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // "Clean Selected" Outlined Button
                            OutlinedButton(
                                onClick = { viewModel.cleanSelectedJunk() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("btn_clean_selected_junk"),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = CustomFlameOrange
                                ),
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
                                        text = "Selected",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // "Clear All" Primary Action Button
                            Button(
                                onClick = { viewModel.cleanAllJunk() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("btn_clean_all_junk"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CustomFlameOrange
                                ),
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
