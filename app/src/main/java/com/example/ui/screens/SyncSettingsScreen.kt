package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.viewmodel.FileManagerViewModel
import com.example.viewmodel.CloudBackupItem
import com.example.viewmodel.FolderCategoryMetadata
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SyncSettingsScreen(
    viewModel: FileManagerViewModel,
    onMenuClick: () -> Unit = {}
) {
    // Current state from the view model
    val selectedService by viewModel.selectedCloudSyncService.collectAsStateWithLifecycle()
    val selectedFolders by viewModel.selectedSyncFolders.collectAsStateWithLifecycle()
    val frequency by viewModel.syncFrequency.collectAsStateWithLifecycle()
    val isFolderSyncActive by viewModel.isFolderSyncActive.collectAsStateWithLifecycle()
    val folderSyncProgress by viewModel.folderSyncProgress.collectAsStateWithLifecycle()
    val folderSyncProgressText by viewModel.folderSyncProgressText.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.lastFolderSyncTime.collectAsStateWithLifecycle()
    val syncError by viewModel.folderSyncError.collectAsStateWithLifecycle()
    val backupsList by viewModel.cloudBackupMetadataList.collectAsStateWithLifecycle()

    // Restore States
    val isRestoreActive by viewModel.isRestoreActive.collectAsStateWithLifecycle()
    val restoreProgress by viewModel.restoreProgress.collectAsStateWithLifecycle()
    val restoreProgressText by viewModel.restoreProgressText.collectAsStateWithLifecycle()
    val restoreSuccess by viewModel.restoreSuccessMessage.collectAsStateWithLifecycle()
    val restoreError by viewModel.restoreErrorMessage.collectAsStateWithLifecycle()

    // Local stats
    val folderMetadata by viewModel.getFolderCategoriesStream().collectAsStateWithLifecycle(emptyList())
    val safeFiles by viewModel.safeFiles.collectAsStateWithLifecycle(initialValue = emptyList())
    val isSafeUnlocked by viewModel.isSafeUnlocked.collectAsStateWithLifecycle()

    // Dialog triggering states
    var showPinDialogForBackup by remember { mutableStateOf(false) }
    var showPinDialogForRestore by remember { mutableStateOf<CloudBackupItem?>(null) }
    
    val isSafeFolderSelected = selectedFolders.contains("Safe Folder")

    val localFolders = remember(folderMetadata, safeFiles) {
        val list = folderMetadata.toMutableList()
        val safeCount = safeFiles?.size ?: 0
        val safeSize = safeFiles?.sumOf { it.size } ?: 0L
        list.add(
            FolderCategoryMetadata(
                name = "Safe Folder",
                fileCount = safeCount,
                totalSize = safeSize
            )
        )
        list
    }

    // Dialogs
    if (showPinDialogForBackup) {
        SyncSettingsPinDialog(
            viewModel = viewModel,
            title = "Secure Backup Handshake",
            subtitle = "Vault files require continuous PIN check before copying to cloud backup.",
            onDismiss = { showPinDialogForBackup = false },
            onVerified = {
                showPinDialogForBackup = false
                viewModel.performFolderBackup()
            }
        )
    }

    showPinDialogForRestore?.let { backupItem ->
        SyncSettingsPinDialog(
            viewModel = viewModel,
            title = "Secure Vault Restore",
            subtitle = "Restore of encrypted Safe Folder files requires verification PIN.",
            onDismiss = { showPinDialogForRestore = null },
            onVerified = {
                showPinDialogForRestore = null
                viewModel.performFolderRestore(backupItem)
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalDarkBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Banner
            SyncSettingsBanner(viewModel = viewModel, onMenuClick = onMenuClick)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // 1. CLOUD DESTINATION SELECTOR COMPONENT
                item {
                    Text(
                        text = "CLOUD SERVICE PROVIDER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = CustomFlameOrange,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    CloudServiceSelector(
                        selectedService = selectedService,
                        onServiceSelected = { service -> viewModel.selectedCloudSyncService.value = service }
                    )
                }

                // 2. BACKUP FREQUENCY COMPONENT (RADIO-BUTTON GROUP)
                item {
                    Text(
                        text = "SYNCHRONIZATION FREQUENCY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = CustomFlameOrange,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    BackupSyncFrequencyRadioGroup(
                        selectedFrequency = frequency,
                        onFrequencySelected = { freq -> viewModel.syncFrequency.value = freq }
                    )
                }

                // 3. SECURE FOLDERS TO BACK UP (CHECKBOX SECTION)
                item {
                    Text(
                        text = "SELECT DIRECTORIES TO SYNCHRONIZE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = CustomFlameOrange,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    DirectoriesSyncSelector(
                        localFolders = localFolders,
                        selectedFolders = selectedFolders,
                        onToggleFolder = { folderName ->
                            val current = selectedFolders.toMutableSet()
                            if (current.contains(folderName)) {
                                current.remove(folderName)
                            } else {
                                current.add(folderName)
                            }
                            viewModel.selectedSyncFolders.value = current
                        },
                        formatFileSize = { size -> viewModel.formatFileSize(size) }
                    )
                }

                // 2.5 NETWORK RESTRICTIONS CONFIGURATION
                item {
                    val simulateWifiOnlySync by viewModel.simulateWifiOnlySync.collectAsStateWithLifecycle()
                    
                    Text(
                        text = "NETWORK & BANDWIDTH OPTIONS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = CustomFlameOrange,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("wifi_only_card"),
                        colors = CardDefaults.cardColors(containerColor = DeepSurfaceDark),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Wifi,
                                        contentDescription = null,
                                        tint = CustomFlameOrange,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Wi-Fi Only Syncing",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Restricts automated sync transfers to Wi-Fi to prevent unintended carrier data fee liabilities.",
                                    fontSize = 11.sp,
                                    color = TextGray
                                )
                            }
                            Switch(
                                checked = simulateWifiOnlySync,
                                onCheckedChange = { viewModel.simulateWifiOnlySync.value = it },
                                modifier = Modifier.testTag("switch_wifi_sync_settings")
                            )
                        }
                    }
                }

                // 4. TRIGGER BACKUP UTILITY
                item {
                    Button(
                        onClick = {
                            if (isSafeFolderSelected && !isSafeUnlocked) {
                                showPinDialogForBackup = true
                            } else {
                                viewModel.performFolderBackup()
                            }
                        },
                        enabled = !isFolderSyncActive && !isRestoreActive && selectedFolders.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CustomFlameOrange,
                            disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_trigger_backup"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isFolderSyncActive) "Syncing..." else "Sync Folders to $selectedService now",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }

                    // Backup Progress Status View
                    if (isFolderSyncActive) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("realtime_sync_progress_card"),
                            colors = CardDefaults.cardColors(containerColor = DeepSurfaceDark),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, CustomFlameOrange.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Sync,
                                            contentDescription = null,
                                            tint = CustomFlameOrange,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Sync Protocol Active",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                    }
                                    Text(
                                        text = "${(folderSyncProgress * 100).toInt()}%",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = CustomFlameOrange
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(20.dp))
                                
                                // Beautiful Central Circular Progress overlay
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(90.dp)
                                ) {
                                    CircularProgressIndicator(
                                        progress = { folderSyncProgress },
                                        modifier = Modifier.fillMaxSize().testTag("sync_progress_circular_indicator"),
                                        color = CustomFlameOrange,
                                        trackColor = Color.White.copy(alpha = 0.08f),
                                        strokeWidth = 6.dp,
                                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                    
                                    // Simulated upload speed inside circle
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "${(3.7 + (folderSyncProgress * 4.2)).toBigDecimal().setScale(1, java.math.RoundingMode.HALF_UP)} MB/s",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Upload",
                                            fontSize = 8.sp,
                                            color = TextGray
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(20.dp))
                                
                                // Current state text
                                Text(
                                    text = folderSyncProgressText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().testTag("sync_progress_text_label")
                                )
                                
                                Spacer(modifier = Modifier.height(14.dp))
                                
                                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                // Two Column metadata metrics
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Transferred", fontSize = 10.sp, color = TextGray)
                                        Text(
                                            text = "${(folderSyncProgress * 28.5).toBigDecimal().setScale(1, java.math.RoundingMode.HALF_UP)} MB",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Box(
                                        modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.05f))
                                    )
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Selected Folders", fontSize = 10.sp, color = TextGray)
                                        Text(
                                            text = "${selectedFolders.size} active",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (syncError != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = syncError ?: "",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    lastSyncTime?.let { time ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Last Synced: ${viewModel.formatDate(time)} at ${SimpleDateFormat("HH:mm", Locale.US).format(Date(time))}",
                            fontSize = 11.sp,
                            color = ForestEcoGreen,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // 5. RESTORE BACKUPS SECTION
                item {
                    Text(
                        text = "RESTORE REST POINTS FROM BACKUPS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = CustomFlameOrange,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )

                    if (backupsList.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DeepSurfaceDark),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.CloudOff,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "No backup restore points found in local Sandbox.\nChoose directories above and tap 'Sync Folders' to create restore nodes.",
                                        fontSize = 11.sp,
                                        color = TextGray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            backupsList.forEach { backup ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = DeepSurfaceDark),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Icon(
                                                imageVector = if (backup.isEncryptedSafeFolder) Icons.Default.Lock else Icons.Default.Folder,
                                                contentDescription = null,
                                                tint = if (backup.isEncryptedSafeFolder) ForestEcoGreen else AquaticWaveBlue,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = backup.folderName,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                    if (backup.isEncryptedSafeFolder) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Icon(
                                                            imageVector = Icons.Default.Security,
                                                            contentDescription = "Encrypted Rest Point",
                                                            tint = ForestEcoGreen,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "${backup.fileCount} files • ${viewModel.formatFileSize(backup.totalSizeBytes)} • via ${backup.cloudService}",
                                                    fontSize = 10.sp,
                                                    color = TextGray
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                if (backup.isEncryptedSafeFolder && !isSafeUnlocked) {
                                                    showPinDialogForRestore = backup
                                                } else {
                                                    viewModel.performFolderRestore(backup)
                                                }
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(AquaticWaveBlue.copy(alpha = 0.12f))
                                                .testTag("btn_restore_${backup.folderName.replace(" ", "_")}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.SettingsBackupRestore,
                                                contentDescription = "Restore this state",
                                                tint = AquaticWaveBlue,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Restore operation status views
                    if (isRestoreActive) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = ForestEcoGreen.copy(alpha = 0.10f)),
                            border = BorderStroke(1.dp, ForestEcoGreen.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    progress = { restoreProgress },
                                    modifier = Modifier.size(18.dp),
                                    color = ForestEcoGreen,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = restoreProgressText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    restoreSuccess?.let { msg ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = ForestEcoGreen.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ForestEcoGreen)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = msg,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    restoreError?.let { err ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = err,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SyncSettingsBanner(
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
            Column {
                Text(
                    text = "SYNC SETTINGS",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = Color.White
                )
                Text(
                    text = "Configure frequencies and active cloud backups",
                    fontSize = 11.sp,
                    color = TextGray
                )
            }
        }
    }
}

@Composable
fun CloudServiceSelector(
    selectedService: String,
    onServiceSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DeepSurfaceDark),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val services = listOf("Google Drive", "Dropbox", "OneDrive")
            services.forEach { service ->
                val isSelected = selectedService == service
                val backgroundColor = if (isSelected) AquaticWaveBlue.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.02f)
                val borderColor = if (isSelected) AquaticWaveBlue else Color.White.copy(alpha = 0.08f)
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(backgroundColor)
                        .clickable { onServiceSelected(service) }
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                        .testTag("srv_$service"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = when (service) {
                                "Google Drive" -> Icons.Default.CloudQueue
                                "Dropbox" -> Icons.Outlined.FolderShared
                                else -> Icons.Default.SyncAlt
                            },
                            contentDescription = null,
                            tint = if (isSelected) AquaticWaveBlue else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = service,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BackupSyncFrequencyRadioGroup(
    selectedFrequency: String,
    onFrequencySelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DeepSurfaceDark),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val frequencies = listOf("Manual", "Daily", "Weekly")
            frequencies.forEach { freq ->
                val isSelected = selectedFrequency == freq
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color.White.copy(alpha = 0.03f) else Color.Transparent)
                        .clickable { onFrequencySelected(freq) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("freq_$freq"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onFrequencySelected(freq) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = CustomFlameOrange,
                            unselectedColor = Color.Gray
                        ),
                        modifier = Modifier.testTag("radio_$freq")
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "$freq Sync",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = when (freq) {
                                "Manual" -> "Syncing only fires when you manually select/tap synchronization utilities."
                                "Daily" -> "Every 24 hours, local directory updates are securely synced in the background."
                                else -> "Once per calendar week, automatic incremental backups are triggered."
                            },
                            fontSize = 10.sp,
                            color = TextGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DirectoriesSyncSelector(
    localFolders: List<FolderCategoryMetadata>,
    selectedFolders: Set<String>,
    onToggleFolder: (String) -> Unit,
    formatFileSize: (Long) -> String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DeepSurfaceDark),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            localFolders.forEach { folder ->
                val isChecked = selectedFolders.contains(folder.name)
                val iconColor = when (folder.name) {
                    "Documents" -> AquaticWaveBlue
                    "Images" -> CustomFlameOrange
                    "Audio" -> ForestEcoGreen
                    "Videos" -> MaterialTheme.colorScheme.primary
                    "Safe Folder" -> ForestEcoGreen
                    else -> Color.Gray
                }
                val folderIcon = when (folder.name) {
                    "Documents" -> Icons.Default.Description
                    "Images" -> Icons.Default.Image
                    "Audio" -> Icons.Default.VolumeUp
                    "Videos" -> Icons.Default.Videocam
                    "Safe Folder" -> Icons.Default.Lock
                    else -> Icons.Default.Extension
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.02f))
                        .clickable { onToggleFolder(folder.name) }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { onToggleFolder(folder.name) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = AquaticWaveBlue,
                            checkmarkColor = Color.White
                        ),
                        modifier = Modifier.testTag("chk_${folder.name.replace(" ", "_")}")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = folderIcon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = folder.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (folder.name == "Safe Folder") {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ForestEcoGreen.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "PIN Required",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ForestEcoGreen
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${folder.fileCount} items • ${formatFileSize(folder.totalSize)}",
                            fontSize = 10.sp,
                            color = TextGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SyncSettingsPinDialog(
    viewModel: FileManagerViewModel,
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    onVerified: () -> Unit
) {
    var pinValue by remember { mutableStateOf("") }
    var pinErrorText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSurfaceDark),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ForestEcoGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = ForestEcoGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // PIN digit bubbles
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 4) {
                        val isDigitEntered = i < pinValue.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (isDigitEntered) AquaticWaveBlue else Color.Gray.copy(alpha = 0.3f))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                pinErrorText?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Keyboard Number Pad
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val keys = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("Clear", "0", "Back")
                    )
                    keys.forEach { rowKeys ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowKeys.forEach { key ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .clickable {
                                            if (key == "Clear") {
                                                pinValue = ""
                                                pinErrorText = null
                                            } else if (key == "Back") {
                                                if (pinValue.isNotEmpty()) {
                                                    pinValue = pinValue.dropLast(1)
                                                    pinErrorText = null
                                                }
                                            } else {
                                                if (pinValue.length < 4) {
                                                    pinValue += key
                                                    pinErrorText = null
                                                    if (pinValue.length == 4) {
                                                        scope.launch {
                                                            val correct = viewModel.verifyAndUnlockSafePin(pinValue)
                                                            if (correct) {
                                                                onVerified()
                                                            } else {
                                                                pinValue = ""
                                                                pinErrorText = "Incorrect 4-Digit PIN sequence!"
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        .padding(vertical = 12.dp)
                                        .testTag("sync_pin_$key"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = key,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Dismiss Protection", color = Color.White)
                }
            }
        }
    }
}
