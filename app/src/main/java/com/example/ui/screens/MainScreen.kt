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
import com.example.GlobalProfileAvatarButton
import com.example.viewmodel.PinMode
import com.example.viewmodel.JunkItem
import androidx.compose.ui.text.TextStyle
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    viewModel: FileManagerViewModel,
    onMenuClick: () -> Unit = {} // Force clear cache comment
) {
    val normalFiles by viewModel.normalFiles.collectAsStateWithLifecycle()
    val junkFiles by viewModel.junkFiles.collectAsStateWithLifecycle()
    val safeFiles by viewModel.safeFiles.collectAsStateWithLifecycle()
    
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.calculateSearchMatchesFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    
    val isAiSearchMode by viewModel.isAiSearchMode.collectAsStateWithLifecycle()
    val isAiSearching by viewModel.isAiSearching.collectAsStateWithLifecycle()
    val aiSearchError by viewModel.aiSearchError.collectAsStateWithLifecycle()
    
    val selectedLocalFileIds by viewModel.selectedLocalFileIds.collectAsStateWithLifecycle()
    val isMultiSelect by viewModel.isMultiSelect.collectAsStateWithLifecycle()
    val filePreview by viewModel.filePreview.collectAsStateWithLifecycle()

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
        filePreview?.let { previewFile ->
            com.example.ui.components.FilePreviewDialog(
                previewFile = previewFile,
                viewModel = viewModel,
                onDismiss = { viewModel.closeFilePreview() }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 8.dp) // Bottom padding
        ) {
            // App Core Branding Header
            BrandingHeader(viewModel = viewModel, onMenuClick = onMenuClick)

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
                    isAiSearchMode = isAiSearchMode,
                    isAiSearching = isAiSearching,
                    aiSearchError = aiSearchError,
                    onAiSearchClick = { viewModel.performGeminiNaturalLanguageSearch(searchQuery) },
                    onClearAiSearch = { viewModel.clearAiSearch() },
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
fun BrandingHeader(
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
            .padding(top = 16.dp, start = 12.dp, end = 16.dp, bottom = 12.dp)
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
                    contentDescription = "Open Navigation Menu",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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

                // Global profile switcher avatar
                GlobalProfileAvatarButton(viewModel = viewModel)
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

    val imagesSize = normalFiles.filter { it.category == "Images" }.sumOf { it.size }
    val videosSize = normalFiles.filter { it.category == "Videos" }.sumOf { it.size }
    val documentsSize = normalFiles.filter { it.category == "Documents" }.sumOf { it.size }
    val othersSize = normalFiles.filter { it.category != "Images" && it.category != "Videos" && it.category != "Documents" }.sumOf { it.size }

    val selectedPartition by viewModel.selectedStoragePartition.collectAsStateWithLifecycle()

    val internalTotal by viewModel.internalTotalSpace.collectAsStateWithLifecycle()
    val internalUsed by viewModel.internalUsedSpace.collectAsStateWithLifecycle()
    
    val sdTotal by viewModel.sdCardTotalSpace.collectAsStateWithLifecycle()
    val sdUsed by viewModel.sdCardUsedSpace.collectAsStateWithLifecycle()

    val activeTotal = if (selectedPartition == "SD Card") sdTotal else internalTotal
    val activeUsed = if (selectedPartition == "SD Card") sdUsed else internalUsed
    val usedRatio = (activeUsed.toFloat() / activeTotal.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Dynamic Storage Switcher (Internal storage vs SD Card)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Internal storage option
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedPartition == "Internal") CosmicPrimary else Color.Transparent)
                        .clickable { viewModel.selectedStoragePartition.value = "Internal" }
                        .padding(vertical = 8.dp)
                        .testTag("tab_storage_internal")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = if (selectedPartition == "Internal") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Internal Space",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedPartition == "Internal") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // SD Card storage option
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedPartition == "SD Card") ForestEcoGreen else Color.Transparent)
                        .clickable { viewModel.selectedStoragePartition.value = "SD Card" }
                        .padding(vertical = 8.dp)
                        .testTag("tab_storage_sd")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SdCard,
                            contentDescription = null,
                            tint = if (selectedPartition == "SD Card") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SD Card Slot",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedPartition == "SD Card") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (selectedPartition == "SD Card") "SD Card Active Storage" else "Internal Shared Storage",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${viewModel.formatFileSize(activeUsed)} / ${viewModel.formatFileSize(activeTotal)} used",
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

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "File Type Distribution",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            
            DiskUsagePieChart(
                imagesSize = imagesSize,
                videosSize = videosSize,
                documentsSize = documentsSize,
                othersSize = othersSize,
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun DiskUsagePieChart(
    imagesSize: Long,
    videosSize: Long,
    documentsSize: Long,
    othersSize: Long,
    viewModel: FileManagerViewModel
) {
    val totalSize = imagesSize + videosSize + documentsSize + othersSize
    if (totalSize == 0L) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No files found. Add mock files to see breakdown.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )
        }
        return
    }

    val imagesPct = imagesSize.toFloat() / totalSize
    val videosPct = videosSize.toFloat() / totalSize
    val documentsPct = documentsSize.toFloat() / totalSize
    val othersPct = othersSize.toFloat() / totalSize

    val slices = listOf(
        Pair(imagesPct, CustomFlameOrange), // Images
        Pair(videosPct, CosmicPrimary),     // Videos
        Pair(documentsPct, AquaticWaveBlue),// Documents
        Pair(othersPct, Color.Gray)         // Others
    ).filter { it.first > 0f }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Draw Chart
        Box(
            modifier = Modifier
                .size(110.dp)
                .testTag("disk_usage_pie_chart"),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = -90f
                slices.forEach { (pct, color) ->
                    val sweepAngle = pct * 360f
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 12.dp.toPx(), 
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )
                    startAngle += sweepAngle
                }
            }
            // Text in center of Donut
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = viewModel.formatFileSize(totalSize),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = Color.White
                )
                Text(
                    text = "Total Files",
                    fontSize = 9.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(20.dp))

        // Legend details
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.wrapContentWidth()
        ) {
            LegendRowItem(color = CustomFlameOrange, label = "Images", pct = imagesPct, sizeStr = viewModel.formatFileSize(imagesSize))
            LegendRowItem(color = CosmicPrimary, label = "Videos", pct = videosPct, sizeStr = viewModel.formatFileSize(videosSize))
            LegendRowItem(color = AquaticWaveBlue, label = "Documents", pct = documentsPct, sizeStr = viewModel.formatFileSize(documentsSize))
            LegendRowItem(color = Color.Gray, label = "Others", pct = othersPct, sizeStr = viewModel.formatFileSize(othersSize))
        }
    }
}

@Composable
fun LegendRowItem(color: Color, label: String, pct: Float, sizeStr: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$label (${(pct * 100).toInt()}%):",
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = Color.LightGray,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = sizeStr,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
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
    isAiSearchMode: Boolean,
    isAiSearching: Boolean,
    aiSearchError: String?,
    onAiSearchClick: () -> Unit,
    onClearAiSearch: () -> Unit,
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    onQueryChange(it)
                    if (isAiSearchMode && it.isBlank()) {
                        onClearAiSearch()
                    }
                },
                placeholder = { 
                    Text(
                        if (isAiSearchMode) "Describe file to Gemini..." 
                        else "Search files (name, custom fuzzy matching)..."
                    ) 
                },
                leadingIcon = { 
                    Icon(
                        imageVector = if (isAiSearchMode) Icons.Default.AutoAwesome else Icons.Default.Search, 
                        contentDescription = "Search Icon", 
                        tint = if (isAiSearchMode) CustomFlameOrange else AquaticWaveBlue
                    ) 
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { 
                                onQueryChange("") 
                                if (isAiSearchMode) {
                                    onClearAiSearch()
                                }
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear Input", tint = Color.Gray)
                            }
                        }
                        
                        // Small Action Sparkle Button for triggering Gemini AI search directly from text field
                        IconButton(
                            onClick = {
                                if (isAiSearchMode) {
                                    onClearAiSearch()
                                } else {
                                    onAiSearchClick()
                                }
                            },
                            modifier = Modifier.testTag("ai_search_toggle_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Search Trigger",
                                tint = if (isAiSearchMode) CustomFlameOrange else Color.Gray
                            )
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_field_input"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isAiSearchMode) CustomFlameOrange else AquaticWaveBlue,
                    unfocusedBorderColor = if (isAiSearchMode) CustomFlameOrange.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = DeepSurfaceDark,
                    unfocusedContainerColor = DeepSurfaceDark
                ),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // AI search details/actions row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isAiSearching) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = CustomFlameOrange
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Gemini processing natural language context...",
                        fontSize = 12.sp,
                        color = CustomFlameOrange,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else if (isAiSearchMode) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CustomFlameOrange.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = CustomFlameOrange,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AI Matches Filtered",
                        color = CustomFlameOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear AI filter",
                        tint = CustomFlameOrange,
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { onClearAiSearch() }
                    )
                }
            } else {
                // Inline Tip Box or Trigger Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onAiSearchClick() }
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = CustomFlameOrange,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Try AI Search (e.g., 'find bill from last week')",
                        color = Color.LightGray.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (isAiSearchMode) {
                Text(
                    text = "Clear AI Filter",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onClearAiSearch() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            } else if (query.trim().isNotEmpty()) {
                Text(
                    text = "Submit AI Query",
                    color = CustomFlameOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onAiSearchClick() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .testTag("submit_ai_search_btn")
                )
            }
        }

        if (aiSearchError != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Error: $aiSearchError",
                color = MaterialTheme.colorScheme.error,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
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
    val explorerMode by viewModel.fileExplorerMode.collectAsStateWithLifecycle()
    val selectedFolder by viewModel.explorerSelectedFolder.collectAsStateWithLifecycle()
    val folderMetadata by viewModel.getFolderCategoriesStream().collectAsStateWithLifecycle(emptyList())

    Column(modifier = modifier.fillMaxWidth()) {
        // Mode switch row: Folders vs Flat files List
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Folders view option
            FilterChip(
                selected = explorerMode == "Folders",
                onClick = { viewModel.fileExplorerMode.value = "Folders" },
                label = { Text("Folders View", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier.testTag("explorer_tab_folders"),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CosmicPrimary,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White
                )
            )

            // Flat files list option
            FilterChip(
                selected = explorerMode == "Flat",
                onClick = { viewModel.fileExplorerMode.value = "Flat" },
                label = { Text("Flat List View", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier.testTag("explorer_tab_flat"),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CosmicPrimary,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            var expanded by remember { mutableStateOf(false) }
            val sortOption by viewModel.fileSortOption.collectAsStateWithLifecycle()

            Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                AssistChip(
                    onClick = { expanded = true },
                    label = { 
                        Text(
                            text = when (sortOption) {
                                "Name" -> "Name"
                                "Size" -> "Size"
                                else -> "Date"
                            }, 
                            fontSize = 11.sp, 
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ) 
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort Files",
                            modifier = Modifier.size(16.dp),
                            tint = CustomFlameOrange
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = Color.White,
                        leadingIconContentColor = CustomFlameOrange
                    ),
                    modifier = Modifier.testTag("btn_file_sort_dropdown_trigger")
                )

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(DeepSurfaceDark).testTag("dropdown_file_sort")
                ) {
                    DropdownMenuItem(
                        text = { Text("Sort by Name", color = Color.White, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.SortByAlpha, contentDescription = null, tint = CustomFlameOrange, modifier = Modifier.size(16.dp)) },
                        onClick = {
                            viewModel.fileSortOption.value = "Name"
                            expanded = false
                        },
                        modifier = Modifier.testTag("sort_option_name")
                    )
                    DropdownMenuItem(
                        text = { Text("Sort by Date Modified", color = Color.White, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = AquaticWaveBlue, modifier = Modifier.size(16.dp)) },
                        onClick = {
                            viewModel.fileSortOption.value = "Date"
                            expanded = false
                        },
                        modifier = Modifier.testTag("sort_option_date")
                    )
                    DropdownMenuItem(
                        text = { Text("Sort by Size", color = Color.White, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.SwapVert, contentDescription = null, tint = ForestEcoGreen, modifier = Modifier.size(16.dp)) },
                        onClick = {
                            viewModel.fileSortOption.value = "Size"
                            expanded = false
                        },
                        modifier = Modifier.testTag("sort_option_size")
                    )
                }
            }
        }

        // Selected breadcrumb trail when drilled down
        if (explorerMode == "Folders" && selectedFolder != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.explorerSelectedFolder.value = null },
                    modifier = Modifier.size(28.dp).testTag("explorer_back_to_root")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to folders",
                        tint = CustomFlameOrange,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Root",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { viewModel.explorerSelectedFolder.value = null }
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = selectedFolder ?: "",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = CustomFlameOrange
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.5f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // If in Folders mode and no folder category is selected -> Show the gorgeous virtual folders grid!
            if (explorerMode == "Folders" && selectedFolder == null) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Directory Folders",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(folderMetadata) { folder ->
                    val folderColor = when (folder.name) {
                        "Documents" -> AquaticWaveBlue
                        "Images" -> CustomFlameOrange
                        "Audio" -> ForestEcoGreen
                        "Videos" -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline
                    }
                    val folderIcon = when (folder.name) {
                        "Documents" -> Icons.Default.Description
                        "Images" -> Icons.Default.Image
                        "Audio" -> Icons.Default.VolumeUp
                        "Videos" -> Icons.Default.Videocam
                        else -> Icons.Default.FolderOpen
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.explorerSelectedFolder.value = folder.name }
                            .testTag("folder_card_${folder.name.lowercase()}"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(folderColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = folderIcon,
                                    contentDescription = folder.name,
                                    tint = folderColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = folder.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "${folder.fileCount} files • ${viewModel.formatFileSize(folder.totalSize)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Open folder",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            } else {
                // List the matched files (scoped inside category if appropriate)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (explorerMode == "Folders") "$selectedFolder Contents" else "All Shared Files",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${searchResults.size} item${if (searchResults.size != 1) "s" else ""}",
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
                                        viewModel.showLocalFilePreview(file)
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
                                    overflow = TextOverflow.Ellipsis,
                                    color = Color.White
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
    val isScanning by viewModel.isDuplicateScanning.collectAsStateWithLifecycle()
    val scanProgress by viewModel.duplicateScanProgress.collectAsStateWithLifecycle()
    val hashGroups by viewModel.duplicateHashGroups.collectAsStateWithLifecycle()
    val hashAlgorithm by viewModel.hashAlgorithmChoice.collectAsStateWithLifecycle()
    val normalFiles by viewModel.normalFiles.collectAsStateWithLifecycle()

    // Map tracking checked File ID -> True (checked for deletion)
    val selectedFiles = remember { mutableStateMapOf<Int, Boolean>() }

    // On dialog enter, run initial scan
    LaunchedEffect(Unit) {
        viewModel.startHashDuplicateScan()
    }

    // Recompute total potential reclaim size of checked duplicates
    val selectedList = normalFiles.filter { selectedFiles[it.id] == true }
    val selectedCount = selectedList.size
    val selectedBytesSize = selectedList.sumOf { it.size }

    Dialog(onDismissRequest = { if (!isScanning) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f)
                .padding(vertical = 12.dp)
                .testTag("duplicate_scanner_modal")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header (Title, Close trigger)
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AquaticWaveBlue.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CopyAll,
                                contentDescription = null,
                                tint = AquaticWaveBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Cryptographic Scanner",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "SHA-256 / MD5 signature replication locator",
                                fontSize = 10.sp,
                                color = TextGray
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isScanning,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))

                // Algorithm Selection and Trigger Action Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column {
                        Text(
                            text = "Hashing Protocol",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGray
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            listOf("SHA-256", "MD5").forEach { algo ->
                                val isSelected = hashAlgorithm == algo
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isSelected) AquaticWaveBlue.copy(alpha = 0.15f)
                                            else Color.Transparent
                                        )
                                        .clickable(enabled = !isScanning) {
                                            viewModel.hashAlgorithmChoice.value = algo
                                            viewModel.startHashDuplicateScan()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = algo,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else TextGray
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { viewModel.startHashDuplicateScan() },
                        enabled = !isScanning,
                        colors = ButtonDefaults.buttonColors(containerColor = AquaticWaveBlue),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rescan", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // main Body Content: scanning loader or duplicate groupings
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (isScanning) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(80.dp)
                            ) {
                                CircularProgressIndicator(
                                    progress = { scanProgress },
                                    modifier = Modifier.fillMaxSize(),
                                    strokeWidth = 5.dp,
                                    color = AquaticWaveBlue,
                                    trackColor = Color.White.copy(alpha = 0.08f)
                                )
                                Text(
                                    text = "${(scanProgress * 100).toInt()}%",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Crunching cryptographic database hashes...",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Validating binary signatures against local file index storage profiles",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Scan complete results list
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (hashGroups.isEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 60.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(CircleShape)
                                                .background(ForestEcoGreen.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = ForestEcoGreen,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Perfect Storage Optimization!",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "No duplicate binary matches found using the selected $hashAlgorithm filter.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextGray,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                hashGroups.forEach { (hash, files) ->
                                    item {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                                            shape = RoundedCornerShape(14.dp),
                                            border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = 0.04f))
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                // Group Header: Hash value + quick select controls
                                                Row(
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "Match Group: ${viewModel.formatFileSize(files.first().size)}",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = CustomFlameOrange
                                                        )
                                                        Text(
                                                            text = "Signature: $hash",
                                                            fontSize = 9.sp,
                                                            color = TextGray,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }

                                                    // Multi-select bulk assists
                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        TextButton(
                                                            onClick = {
                                                                // Keeps the first file, auto check the rest
                                                                files.forEachIndexed { idx, file ->
                                                                    selectedFiles[file.id] = idx > 0
                                                                }
                                                            },
                                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                            modifier = Modifier.height(24.dp)
                                                        ) {
                                                            Text("Prune Extras", fontSize = 9.sp, color = AquaticWaveBlue, fontWeight = FontWeight.Bold)
                                                        }
                                                        TextButton(
                                                            onClick = {
                                                                files.forEach { file -> selectedFiles[file.id] = false }
                                                            },
                                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                            modifier = Modifier.height(24.dp)
                                                        ) {
                                                            Text("Deselect", fontSize = 9.sp, color = TextGray)
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))
                                                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                                Spacer(modifier = Modifier.height(6.dp))

                                                // List files in this match group
                                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    files.forEach { file ->
                                                        val isChecked = selectedFiles[file.id] == true
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(
                                                                    if (isChecked) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.08f)
                                                                    else Color.Black.copy(alpha = 0.20f)
                                                                )
                                                                .clickable { viewModel.showLocalFilePreview(file) }
                                                                .padding(vertical = 4.dp, horizontal = 8.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Checkbox(
                                                                checked = isChecked,
                                                                onCheckedChange = { isCheckedNext ->
                                                                    selectedFiles[file.id] = isCheckedNext == true
                                                                },
                                                                colors = CheckboxDefaults.colors(
                                                                    checkedColor = MaterialTheme.colorScheme.error,
                                                                    uncheckedColor = Color.White.copy(alpha = 0.3f),
                                                                    checkmarkColor = Color.White
                                                                ),
                                                                modifier = Modifier.size(36.dp)
                                                            )

                                                            Spacer(modifier = Modifier.width(4.dp))

                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    text = file.name,
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color.White,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                                Text(
                                                                    text = "Location: ${file.path}",
                                                                    fontSize = 9.sp,
                                                                    color = TextGray,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }

                                                            // File category icon representation
                                                            Icon(
                                                                imageVector = when (file.category) {
                                                                    "Images" -> Icons.Default.Image
                                                                    "Audio" -> Icons.Default.Audiotrack
                                                                    "Videos" -> Icons.Default.Movie
                                                                    else -> Icons.Default.Description
                                                                },
                                                                contentDescription = null,
                                                                tint = AquaticWaveBlue.copy(alpha = 0.5f),
                                                                modifier = Modifier
                                                                    .size(16.dp)
                                                                    .padding(horizontal = 4.dp)
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
                    }
                }

                // Dialog Action Footer controls
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Active deletion trigger button
                    if (selectedCount > 0) {
                        Button(
                            onClick = {
                                viewModel.deleteDuplicateFiles(selectedList)
                                selectedFiles.clear()
                            },
                            enabled = !isScanning,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Delete $selectedCount Selected (${viewModel.formatFileSize(selectedBytesSize)})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isScanning,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (selectedCount > 0) "Dismiss" else "Close Scanner",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
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
    var activeTab by remember { mutableStateOf(0) } // 0 = Files, 1 = Backup Settings
    
    val isEnabled by viewModel.isSafeFolderSyncEnabled.collectAsStateWithLifecycle()
    val syncService by viewModel.selectedSafeSyncService.collectAsStateWithLifecycle()
    val destinationFolder by viewModel.safeSyncDestinationFolder.collectAsStateWithLifecycle()
    val isSyncActive by viewModel.isSafeSyncActive.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.lastSafeSyncTime.collectAsStateWithLifecycle()
    val syncError by viewModel.safeSyncError.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab switcher Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Files Tab Button
                Button(
                    onClick = { activeTab = 0 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeTab == 0) ForestEcoGreen else Color.Transparent,
                        contentColor = if (activeTab == 0) Color.Black else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("safe_tab_files")
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Files (${safeFiles.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Cloud Sync Tab Button
                Button(
                    onClick = { activeTab = 1 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeTab == 1) ForestEcoGreen else Color.Transparent,
                        contentColor = if (activeTab == 1) Color.Black else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("safe_tab_sync")
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Backup Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    if (isEnabled) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.Yellow)
                        )
                    }
                }
            }

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

        if (activeTab == 0) {
            // Tab 0: Files List
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.showLocalFilePreview(file) },
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
        } else {
            // Tab 1: Cloud Sync Settings
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Status banner
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isEnabled) ForestEcoGreen.copy(alpha = 0.08f) else Color.Gray.copy(alpha = 0.08f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isEnabled) ForestEcoGreen.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = if (isEnabled) ForestEcoGreen else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isEnabled) "Auto-Cloud Backup Active" else "Cloud Backup Disabled",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isEnabled) "Files are automatically backed up securely to your selected cloud folder." else "Enable auto-sync below to automatically back up your items.",
                                    fontSize = 10.sp,
                                    color = TextGray
                                )
                            }
                        }
                    }
                }

                // AutoSync Switch Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Autorenew,
                                    contentDescription = null,
                                    tint = ForestEcoGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Automatic Cloud Sync", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Sync when file is moved to Safe", fontSize = 10.sp, color = TextGray)
                                }
                            }
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { viewModel.isSafeFolderSyncEnabled.value = it },
                                modifier = Modifier.testTag("safe_switch_auto_sync"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = ForestEcoGreen,
                                    checkedTrackColor = ForestEcoGreen.copy(alpha = 0.4f)
                                )
                            )
                        }
                    }
                }

                // Cloud Provider Selection
                item {
                    Column {
                        Text("Cloud Service Provider", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ForestEcoGreen, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val cloudServices = listOf("Google Drive", "Dropbox", "OneDrive")
                            cloudServices.forEach { service ->
                                val selected = syncService == service
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.selectedSafeSyncService.value = service }
                                        .testTag("safe_provider_$service"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selected) ForestEcoGreen.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (selected) ForestEcoGreen else Color.White.copy(alpha = 0.10f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = service,
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selected) Color.White else TextGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Destination Folder Input
                item {
                    Column {
                        Text("Backup Destination Folder", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ForestEcoGreen, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                        OutlinedTextField(
                            value = destinationFolder,
                            onValueChange = { viewModel.safeSyncDestinationFolder.value = it },
                            placeholder = { Text("e.g. /Secure_Backup", fontSize = 12.sp, color = Color.Gray) },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 12.sp, color = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("safe_destination_input"),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ForestEcoGreen,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedLabelColor = ForestEcoGreen,
                                cursorColor = ForestEcoGreen,
                                focusedContainerColor = Color.White.copy(alpha = 0.02f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                            )
                        )
                    }
                }

                // PIN Protection Security notice
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DeepSurfaceDark.copy(alpha = 0.6f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(modifier = Modifier.padding(10.dp)) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = ForestEcoGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("PIN-Enforced Decryption Protection", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Synced files are protected by your Private Vault PIN in the cloud. Original filenames are safely wrapped and can only be decrypted and opened when you verify your PIN inside this safe.",
                                    fontSize = 9.sp,
                                    lineHeight = 12.sp,
                                    color = TextGray
                                )
                            }
                        }
                    }
                }

                // Manual Backup action section
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { viewModel.syncSafeFolderToCloud() },
                        enabled = !isSyncActive,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ForestEcoGreen,
                            contentColor = Color.Black,
                            disabledContainerColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("safe_sync_now_button")
                    ) {
                        if (isSyncActive) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Encrypting & Backing Up...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Sync info details (Timestamp, Error)
                item {
                    if (syncError != null) {
                        Text(
                            text = syncError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    } else if (lastSyncTime != null) {
                        val sdf = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
                        val formatted = lastSyncTime?.let { Date(it) }?.let { sdf.format(it) } ?: ""
                        Text(
                            text = "Last synchronized to $syncService: $formatted",
                            color = ForestEcoGreen,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            textAlign = TextAlign.Center
                        )
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
