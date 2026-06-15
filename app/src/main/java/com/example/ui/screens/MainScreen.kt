package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.FileEntity
import com.example.ui.theme.*
import com.example.viewmodel.FileManagerViewModel
import com.example.viewmodel.PinMode
import com.example.viewmodel.JunkItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(viewModel: FileManagerViewModel) {
    val normalFiles by viewModel.normalFiles.collectAsStateWithLifecycle()
    val junkFiles by viewModel.junkFiles.collectAsStateWithLifecycle()
    val safeFiles by viewModel.safeFiles.collectAsStateWithLifecycle()
    
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.calculateSearchMatchesFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    
    val selectedLocalFileIds by viewModel.selectedLocalFileIds.collectAsStateWithLifecycle()
    val isMultiSelect by viewModel.isMultiSelect.collectAsStateWithLifecycle()

    // Animation states
    val isJunkCleaning by viewModel.isJunkCleaning.collectAsStateWithLifecycle()
    val showCelebrationDialog by viewModel.showCelebrationDialog.collectAsStateWithLifecycle()
    val junkBytesCleaned by viewModel.junkBytesCleaned.collectAsStateWithLifecycle()
    val showDuplicateScanner by viewModel.showDuplicateScanner.collectAsStateWithLifecycle()
    val showJunkCleaner by viewModel.showJunkCleaner.collectAsStateWithLifecycle()

    // Safe state
    val isPinRegistered by viewModel.isPinRegistered.collectAsStateWithLifecycle()
    val isSafeUnlocked by viewModel.isSafeUnlocked.collectAsStateWithLifecycle()
    val passcodeMode by viewModel.passcodeMode.collectAsStateWithLifecycle()
    val enteredPinBuffer by viewModel.enteredPinBuffer.collectAsStateWithLifecycle()
    val pinErrorMessage by viewModel.pinErrorMessage.collectAsStateWithLifecycle()
    
    var showAddFileDialog by remember { mutableStateOf(false) }
    var inSafeViewMode by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp) // Bottom padding for navigation
        ) {
            // App Core Branding Header
            BrandingHeader()

            if (inSafeViewMode) {
                // Secure Folder Screen Inside Main Area
                SecureFolderSection(
                    viewModel = viewModel,
                    safeFiles = safeFiles,
                    enteredPinBuffer = enteredPinBuffer,
                    passcodeMode = passcodeMode,
                    isPinRegistered = isPinRegistered,
                    isSafeUnlocked = isSafeUnlocked,
                    pinErrorMessage = pinErrorMessage,
                    onBackToNormal = {
                        inSafeViewMode = false
                        viewModel.lockSafeFolder()
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                // Storage Gauge Card
                StorageGaugeCard(
                    normalFiles = normalFiles,
                    junkFiles = junkFiles,
                    safeFiles = safeFiles,
                    viewModel = viewModel
                )

                // Quick smart utilities toolbar
                SmartUtilitiesRow(
                    junkFiles = junkFiles,
                    onCleanJunk = { viewModel.showJunkCleaner.value = true },
                    onOpenDuplicates = { viewModel.showDuplicateScanner.value = true },
                    onOpenSafe = { inSafeViewMode = true }
                )

                // Real-time Search Box
                SearchAndAddBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.searchQuery.value = it },
                    onAddClick = { showAddFileDialog = true }
                )

                // Multiple choice toggle notification
                if (isMultiSelect) {
                    MultiSelectActionBar(
                        selectedCount = selectedLocalFileIds.size,
                        onSelectAll = { viewModel.selectAllNormalFiles() },
                        onClearAll = { viewModel.clearLocalSelection() },
                        onDeleteSelected = { viewModel.deleteSelectedLocalFiles() },
                        onMoveToSafe = { viewModel.moveSelectedToSafe() }
                    )
                }

                // File List
                FileListSection(
                    searchResults = searchResults,
                    selectedIds = selectedLocalFileIds,
                    isMultiSelect = isMultiSelect,
                    onToggleSelectMode = { viewModel.isMultiSelect.value = true },
                    onToggleFile = { viewModel.toggleLocalFileSelection(it.id) },
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Floating Action Button
        if (!inSafeViewMode && !isMultiSelect) {
            FloatingActionButton(
                onClick = { showAddFileDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("fab_add_file"),
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Mock File")
            }
        }

        // Full-screen Junk Cleaning overlay
        if (isJunkCleaning) {
            JunkCleaningOverlay(viewModel.formatFileSize(junkBytesCleaned))
        }

        // Confetti Celebration Dialog
        if (showCelebrationDialog) {
            CelebrationDialog(
                cleanedSize = viewModel.formatFileSize(junkBytesCleaned),
                onDismiss = { viewModel.showCelebrationDialog.value = false }
            )
        }

        // Duplicate Scanner bottom sheet / modal
        if (showDuplicateScanner) {
            DuplicateScannerDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.showDuplicateScanner.value = false }
            )
        }

        // Junk Cleaner Scanner Dialog
        if (showJunkCleaner) {
            JunkCleanerDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.showJunkCleaner.value = false }
            )
        }

        // Add Mock File Dialogue
        if (showAddFileDialog) {
            AddMockFileDialog(
                onDismiss = { showAddFileDialog = false },
                onAddFile = { name, mime, size, cat ->
                    viewModel.addManualLocalFile(name, mime, size, cat)
                    showAddFileDialog = false
                }
            )
        }
    }
}

// --- Visual Branding Header matching Logo vibe ---
@Composable
fun BrandingHeader() {
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
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Left brand icon representing the fire wheel & water logo
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Brush.sweepGradient(listOf(CustomFlameOrange, AquaticWaveBlue, ForestEcoGreen, CustomFlameOrange)))
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(CosmicPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AllInclusive,
                        contentDescription = null,
                        tint = CustomFlameOrange,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "VISHVA SPACE",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    ),
                    color = Color.White
                )
                Text(
                    text = "Smart Local & Cloud Vault",
                    style = MaterialTheme.typography.bodySmall,
                    color = AquaticWaveBlue,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.weight(1.0f))

            // Pulse badge indicator for active protection
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(ForestEcoGreen.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(ForestEcoGreen)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "SECURE",
                    style = MaterialTheme.typography.labelSmall,
                    color = ForestEcoGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// --- Storage Gauge Card displaying local usage statistics ---
@Composable
fun StorageGaugeCard(
    normalFiles: List<FileEntity>,
    junkFiles: List<FileEntity>,
    safeFiles: List<FileEntity>,
    viewModel: FileManagerViewModel
) {
    val totalNormalSize = normalFiles.sumOf { it.size }
    val totalJunkSize = junkFiles.sumOf { it.size }
    val totalSafeSize = safeFiles.sumOf { it.size }
    val simulatedCap = 500L * 1024L * 1024L // 500 MB simulated system cap

    val totalUsed = totalNormalSize + totalJunkSize + totalSafeSize
    val usedRatio = (totalUsed.toFloat() / simulatedCap.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Local Storage",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${viewModel.formatFileSize(totalUsed)} / ${viewModel.formatFileSize(simulatedCap)} used",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Multi-color linear storage gauge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                // Used gauge segments (represented by color layers)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(usedRatio)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(CosmicPrimary, AquaticWaveBlue, CustomFlameOrange)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Storage Legend Categories
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                LegendItem(
                    color = CosmicPrimary,
                    label = "Normal Files",
                    size = viewModel.formatFileSize(totalNormalSize)
                )
                LegendItem(
                    color = CustomFlameOrange,
                    label = "Junk Items",
                    size = viewModel.formatFileSize(totalJunkSize)
                )
                LegendItem(
                    color = ForestEcoGreen,
                    label = "Private Vault",
                    size = viewModel.formatFileSize(totalSafeSize)
                )
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String, size: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = size,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// --- Quick Utilities Toolbar Card Row ---
@Composable
fun SmartUtilitiesRow(
    junkFiles: List<FileEntity>,
    onCleanJunk: () -> Unit,
    onOpenDuplicates: () -> Unit,
    onOpenSafe: () -> Unit
) {
    val junkCount = junkFiles.size
    val totalJunkSizeString = if (junkCount > 0) {
        "${junkFiles.sumOf { it.size } / (1024 * 1024)} MB"
    } else "0 MB"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Junk Utility
        UtilityButton(
            icon = Icons.Default.DeleteSweep,
            title = "Clean Junk",
            subtitle = if (junkCount > 0) "$junkCount files ($totalJunkSizeString)" else "Cleaned",
            accentColor = CustomFlameOrange,
            modifier = Modifier.weight(1f),
            onClick = onCleanJunk
        )

        // Duplicates Scan Utility
        UtilityButton(
            icon = Icons.Default.FilterNone,
            title = "Duplicates",
            subtitle = "Scan matches",
            accentColor = AquaticWaveBlue,
            modifier = Modifier.weight(1f),
            onClick = onOpenDuplicates
        )

        // Safe Private Vault Utility
        UtilityButton(
            icon = Icons.Default.Shield,
            title = "Secure Folder",
            subtitle = "Custom PIN",
            accentColor = ForestEcoGreen,
            modifier = Modifier.weight(1f),
            onClick = onOpenSafe
        )
    }
}

@Composable
fun UtilityButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// --- Search and Add File Input Field ---
@Composable
fun SearchAndAddBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onAddClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search files with custom fuzzy percentages...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = AquaticWaveBlue) },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            } else null,
            modifier = Modifier
                .weight(1f)
                .testTag("search_field_input"),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CustomFlameOrange,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            singleLine = true
        )
    }
}

// --- Multi Selection Control Toolbar ---
@Composable
fun MultiSelectActionBar(
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    onMoveToSafe: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClearAll) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel MultiSelect")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$selectedCount Selected",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSelectAll,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Text("Select All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = onMoveToSafe,
                    modifier = Modifier.testTag("action_move_safe")
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = "Lock to Safe",
                        tint = ForestEcoGreen
                    )
                }

                IconButton(
                    onClick = onDeleteSelected,
                    modifier = Modifier.testTag("action_delete_batch")
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Batch Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// --- File Scroll list with custom score match badges ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListSection(
    searchResults: List<Pair<FileEntity, Double>>,
    selectedIds: Set<Int>,
    isMultiSelect: Boolean,
    onToggleSelectMode: () -> Unit,
    onToggleFile: (FileEntity) -> Unit,
    viewModel: FileManagerViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Items List",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${searchResults.size} matches available",
                    style = MaterialTheme.typography.bodySmall,
                    color = AquaticWaveBlue
                )
            }
        }

        if (searchResults.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No files match your query.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        items(searchResults, key = { it.first.id }) { (file, matchScore) ->
            val isSelected = selectedIds.contains(file.id)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onLongClick = {
                            if (!isMultiSelect) {
                                onToggleSelectMode()
                                onToggleFile(file)
                            }
                        },
                        onClick = {
                            if (isMultiSelect) {
                                onToggleFile(file)
                            } else {
                                // Informative short tap snack feedback
                            }
                        }
                    )
                    .testTag("file_row_card_${file.id}"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    }
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isMultiSelect) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleFile(file) },
                            modifier = Modifier.testTag("checkbox_${file.id}")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    // Circular icon based on type
                    val categoryIcon = when (file.category) {
                        "Documents" -> Icons.Default.Description
                        "Images" -> Icons.Default.Image
                        "Audio" -> Icons.Default.VolumeUp
                        "Videos" -> Icons.Default.Videocam
                        else -> Icons.Default.InsertDriveFile
                    }
                    val categoryColor = when (file.category) {
                        "Documents" -> AquaticWaveBlue
                        "Images" -> CustomFlameOrange
                        "Audio" -> ForestEcoGreen
                        "Videos" -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(categoryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = file.category,
                            tint = categoryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${file.category} • ${viewModel.formatFileSize(file.size)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Matching Score Indicator Badge (if search query actively typed)
                    if (matchScore < 100.0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AquaticWaveBlue.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${matchScore.toInt()}% match",
                                fontSize = 10.sp,
                                color = AquaticWaveBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // Regular quick trash icon for normal mode delete
                        if (!isMultiSelect) {
                            IconButton(onClick = { viewModel.deleteLocalFileDirectly(file.id) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete File",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- FULLSCREEN JUNK CLEANING OVERLAY ---
@Composable
fun JunkCleaningOverlay(cleanedSizeString: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "CleanRotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalDarkBg.copy(alpha = 0.9f))
            .testTag("junk_cleaning_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Rotating Broom/Brush symbol
            Icon(
                imageVector = Icons.Default.Autorenew,
                contentDescription = "Cleaning in Progress",
                tint = CustomFlameOrange,
                modifier = Modifier
                    .size(96.dp)
                    .rotate(rotationAngle)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Clearing Space Artifacts...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Scrubbing cache, logs, and compiled remnants from disk.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                color = CustomFlameOrange,
                trackColor = CosmicPrimary.copy(alpha = 0.3f),
                modifier = Modifier
                    .width(180.dp)
                    .clip(CircleShape)
            )
        }
    }
}

// --- CONFETTI CELEBRATION COMPLETED DIALOG ---
@Composable
fun CelebrationDialog(cleanedSize: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSurfaceDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("celebration_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large smiling celebratory checkmark icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(ForestEcoGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Celebration,
                        contentDescription = null,
                        tint = CustomFlameOrange,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "System Clean Completed!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Successfully reclaimed $cleanedSize of dead storage files. Your phone cache files have been cleared.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = CustomFlameOrange),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("De-clutter Done! 🎉", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- DUPLICATE SCANNER VISUAL DIALOG ---
@Composable
fun DuplicateScannerDialog(
    viewModel: FileManagerViewModel,
    onDismiss: () -> Unit
) {
    val duplicateGroupings = viewModel.getDuplicateFileGroupings()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSurfaceDark),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(vertical = 16.dp)
                .testTag("duplicate_scanner_modal")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CopyAll,
                            contentDescription = null,
                            tint = AquaticWaveBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Duplicate Scanner",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "The duplicate scanning engine located matches based on identical file sizes. Remove secondary copies to clean storage space.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (duplicateGroupings.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ForestEcoGreen, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No duplicate files found! Excellent efficiency.", style = MaterialTheme.typography.bodyMedium, color = TextGray)
                            }
                        }
                    }

                    duplicateGroupings.forEach { (size, files) ->
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Match Group (${viewModel.formatFileSize(size)})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CustomFlameOrange
                                        )
                                        Text(
                                            text = "${files.size} duplicates",
                                            fontSize = 10.sp,
                                            color = TextGray
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        files.forEach { file ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        Color.Black.copy(alpha = 0.2f),
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .padding(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.InsertDriveFile,
                                                    contentDescription = null,
                                                    tint = AquaticWaveBlue,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = file.name,
                                                    fontSize = 11.sp,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                IconButton(
                                                    onClick = { viewModel.deleteDuplicateFile(file) },
                                                    modifier = Modifier.size(24.dp).testTag("delete_duplicate_btn_${file.id}")
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Delete secondary copy",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Text("Close Scanner", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun JunkCleanerDialog(
    viewModel: FileManagerViewModel,
    onDismiss: () -> Unit
) {
    val isJunkScanning by viewModel.isJunkScanning.collectAsStateWithLifecycle()
    val scannedJunkItems by viewModel.scannedJunkItems.collectAsStateWithLifecycle()
    
    // Trigger scan on entry
    LaunchedEffect(Unit) {
        viewModel.startJunkScan()
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSurfaceDark),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(vertical = 16.dp)
                .testTag("junk_cleaner_dialog")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                
                // Header Row
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = CustomFlameOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Junk Clean Scanner",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                if (isJunkScanning) {
                    // Animating Scanning Circle/Text
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = CustomFlameOrange, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Scanning system directories...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Searching for residual caches & empty folders.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    }
                } else {
                    val checkedItems = scannedJunkItems.filter { it.isChecked }
                    val totalReclaimableSize = checkedItems.sumOf { it.size }
                    val cacheItems = scannedJunkItems.filter { !it.isFolder }
                    val emptyFolders = scannedJunkItems.filter { it.isFolder }
                    
                    Text(
                        text = "Scanned and found ${scannedJunkItems.size} items. Selected items will be deleted permanently.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Section: Cache files
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
                                JunkItemRow(item = item, onToggle = { viewModel.toggleJunkItem(item.id) }, viewModel = viewModel)
                            }
                        }
                        
                        // Section: Empty folders
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
                                JunkItemRow(item = item, onToggle = { viewModel.toggleJunkItem(item.id) }, viewModel = viewModel)
                            }
                        }
                        
                        if (scannedJunkItems.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No junk items or empty folders remain!", color = TextGray)
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Clean now button
                    Button(
                        onClick = { viewModel.cleanSelectedJunk() },
                        colors = ButtonDefaults.buttonColors(containerColor = CustomFlameOrange),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("button_clean_now_junk"),
                        enabled = checkedItems.isNotEmpty()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Clean Now (${viewModel.formatFileSize(totalReclaimableSize)})",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JunkItemRow(
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
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = if (item.isFolder) "0 B" else viewModel.formatFileSize(item.size),
            fontSize = 12.sp,
            color = if (item.isFolder) AquaticWaveBlue else Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Checkbox(
            checked = item.isChecked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = CustomFlameOrange,
                uncheckedColor = TextGray,
                checkmarkColor = Color.White
            )
        )
    }
}

// --- SECURE SAFE FOLDER SYSTEM INNER FRAME ---
@Composable
fun SecureFolderSection(
    viewModel: FileManagerViewModel,
    safeFiles: List<FileEntity>,
    enteredPinBuffer: String,
    passcodeMode: PinMode,
    isPinRegistered: Boolean,
    isSafeUnlocked: Boolean,
    pinErrorMessage: String?,
    onBackToNormal: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSurfaceDark),
            shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header bar inside folder
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = ForestEcoGreen,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Private Vault",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                IconButton(onClick = onBackToNormal) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Exit Safe Folder", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!isSafeUnlocked) {
                // PIN State Machine interface (Lock states)
                PasscodeInterface(
                    enteredPinBuffer = enteredPinBuffer,
                    passcodeMode = passcodeMode,
                    isPinRegistered = isPinRegistered,
                    pinErrorMessage = pinErrorMessage,
                    viewModel = viewModel
                )
            } else {
                // Unlocked View State! Show Private File metadata
                UnlockedSafeFolderContents(
                    safeFiles = safeFiles,
                    viewModel = viewModel,
                    onLockSafe = { viewModel.lockSafeFolder() },
                    onResetSecret = { viewModel.resetSafePinConfig() }
                )
            }
        }
    }
}

@Composable
fun PasscodeInterface(
    enteredPinBuffer: String,
    passcodeMode: PinMode,
    isPinRegistered: Boolean,
    pinErrorMessage: String?,
    viewModel: FileManagerViewModel
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val titleText = when (passcodeMode) {
            PinMode.Register -> "Set 4-Digit Private PIN"
            PinMode.Confirm -> "Re-type Private PIN to Confirm"
            PinMode.EnterPin -> "Unlock Vault Security"
        }
        val descText = when (passcodeMode) {
            PinMode.Register -> "Establish a private password. Any documents locked here cannot be normal-scanned."
            PinMode.Confirm -> "Ensure your digits align perfectly to construct the database row security."
            PinMode.EnterPin -> "Enter your custom 4-digit PIN sequence to unhide your encrypted files."
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = titleText,
            style = MaterialTheme.typography.titleSmall,
            color = CustomFlameOrange,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = descText,
            fontSize = 11.sp,
            color = TextGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Visual passcode circles (dots showing entries)
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0..3) {
                val isFilled = i < enteredPinBuffer.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (isFilled) CustomFlameOrange else Color.Gray.copy(alpha = 0.4f)
                        )
                )
            }
        }

        if (pinErrorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = pinErrorMessage,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(1.0f))

        // Custom Numeric Keypad
        NumericKeypad(
            onDigitClick = { viewModel.submitPinDigit(it) },
            onDeleteClick = { viewModel.backspacePinDigit() },
            onClearClick = { viewModel.clearPinBuffer() }
        )
    }
}

@Composable
fun NumericKeypad(
    onDigitClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onClearClick: () -> Unit
) {
    val keypadStructure = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("C", "0", "⌫")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(bottom = 12.dp)
    ) {
        keypadStructure.forEach { rowList ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                rowList.forEach { char ->
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .clickable {
                                when (char) {
                                    "C" -> onClearClick()
                                    "⌫" -> onDeleteClick()
                                    else -> onDigitClick(char)
                                }
                            }
                            .testTag("pin_key_$char"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = if (char == "C" || char == "⌫") CustomFlameOrange else Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UnlockedSafeFolderContents(
    safeFiles: List<FileEntity>,
    viewModel: FileManagerViewModel,
    onLockSafe: () -> Unit,
    onResetSecret: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Secure Vault Files (${safeFiles.size})",
                style = MaterialTheme.typography.bodySmall,
                color = ForestEcoGreen,
                fontWeight = FontWeight.Bold
            )

            Row {
                IconButton(onClick = onLockSafe) {
                    Icon(Icons.Default.Lock, contentDescription = "Lock Safe", tint = CustomFlameOrange)
                }
                IconButton(onClick = onResetSecret) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset PIN", tint = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (safeFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Secure Vault is Empty.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "To move sensitive items here, long-press local files and select 'Move to Safe Folder'.",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(safeFiles, key = { it.id }) { file ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(10.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Encrypted Item",
                                tint = ForestEcoGreen,
                                modifier = Modifier.size(18.dp)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${file.category} • ${viewModel.formatFileSize(file.size)}",
                                    fontSize = 10.sp,
                                    color = TextGray
                                )
                            }

                            // Restore and Trash controls
                            IconButton(onClick = { viewModel.restoreFromSafe(file.id) }) {
                                Icon(
                                    Icons.Default.RestorePage,
                                    contentDescription = "Unhide File",
                                    tint = AquaticWaveBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(onClick = { viewModel.deleteSafeFile(file) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete File Permanently",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- ADD MOCK FILE WORK LAYOUT DIALOG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMockFileDialog(
    onDismiss: () -> Unit,
    onAddFile: (String, String, Long, String) -> Unit
) {
    var rawName by remember { mutableStateOf("") }
    var rawSizeString by remember { mutableStateOf("4500000") } // default size (e.g. ~4.5MB)
    var selectedCategory by remember { mutableStateOf("Documents") }
    
    val catList = listOf("Documents", "Images", "Audio", "Videos")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSurfaceDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("add_file_dialog")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Insert Mock Space File",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = rawName,
                    onValueChange = { rawName = it },
                    label = { Text("Filename (e.g., trust_ledger.pdf)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedLabelColor = CustomFlameOrange,
                        focusedBorderColor = CustomFlameOrange
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_mock_name")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = rawSizeString,
                    onValueChange = { rawSizeString = it },
                    label = { Text("File size in bytes") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedLabelColor = CustomFlameOrange,
                        focusedBorderColor = CustomFlameOrange
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_mock_size")
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Select space segment category:", fontSize = 11.sp, color = TextGray)
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    catList.forEach { category ->
                        val isSelected = selectedCategory == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) CustomFlameOrange else CosmicPrimary.copy(alpha = 0.4f)
                                )
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("cat_option_$category"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = category,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Color.White)
                    }

                    Button(
                        onClick = {
                            val name = if (rawName.isBlank()) "new_mock_file.docx" else rawName
                            val size = rawSizeString.toLongOrNull() ?: 500000L
                            val mime = when (selectedCategory) {
                                "Documents" -> "application/pdf"
                                "Images" -> "image/png"
                                "Audio" -> "audio/mpeg"
                                "Videos" -> "video/mp4"
                                else -> "text/plain"
                            }
                            onAddFile(name, mime, size, selectedCategory)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CustomFlameOrange),
                        modifier = Modifier.weight(1f).testTag("btn_confirm_add")
                    ) {
                        Text("Create", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
