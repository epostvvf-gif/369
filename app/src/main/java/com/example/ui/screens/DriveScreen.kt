package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.viewmodel.CloudFile
import com.example.viewmodel.FileManagerViewModel
import com.example.GlobalProfileAvatarButton

@Composable
fun DriveScreen(
    viewModel: FileManagerViewModel,
    onMenuClick: () -> Unit = {} // Force clear cache comment
) {
    val cloudFiles by viewModel.cloudFiles.collectAsStateWithLifecycle()
    val accounts by viewModel.cloudAccounts.collectAsStateWithLifecycle()
    val activeAccount by viewModel.selectedCloudAccount.collectAsStateWithLifecycle()
    val searchCloudQuery by viewModel.searchCloudQuery.collectAsStateWithLifecycle()
    val selectedCloudFileIds by viewModel.selectedCloudFileIds.collectAsStateWithLifecycle()
    val isScanning by viewModel.isCloudScanning.collectAsStateWithLifecycle()
    val scanProgress by viewModel.cloudScanProgress.collectAsStateWithLifecycle()

    var showUploadCloudDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 8.dp) // Leave height for navigation bar
        ) {
            // Screen Title Banner
            CloudBanner(viewModel = viewModel, onMenuClick = onMenuClick)

            val accountVal = activeAccount
            if (accountVal == null) {
                // If logged out - show OAuth simulated login screen
                CloudLoginSimScreen(
                    onLoginDefault = {
                        viewModel.addCloudAccount("epostvvf@gmail.com")
                    },
                    onLoginCustom = { email ->
                        viewModel.addCloudAccount(email)
                    }
                )
            } else {
                // Display Active account switcher info card
                ActiveAccountProfileCard(
                    account = accountVal,
                    onSwitchClick = { viewModel.showGlobalAccountSwitcher.value = true }
                )

                // Cloud Storage & Sync Dashboard Status simulation
                CloudStorageDashboardCard(
                    cloudFiles = cloudFiles,
                    viewModel = viewModel
                )

                // High-Thinking Semantic AI Scan dashboard card
                SemanticScanDashboardCard(
                    isScanning = isScanning,
                    progress = scanProgress,
                    onStartScan = {
                        if (viewModel.geminiApiKey.value.isBlank()) {
                            viewModel.showApiKeyPromptDialogForScan.value = true
                        } else {
                            viewModel.startSemanticScan()
                        }
                    }
                )

                // Search Cloud files Bar
                CloudSearchBar(
                    query = searchCloudQuery,
                    onQueryChange = { viewModel.searchCloudQuery.value = it },
                    onUploadClick = { showUploadCloudDialog = true }
                )

                // Cloud batch action card
                if (selectedCloudFileIds.isNotEmpty()) {
                    CloudBatchActionsBar(
                        selectedCount = selectedCloudFileIds.size,
                        onClearSelection = { viewModel.selectedCloudFileIds.value = emptySet() },
                        onDeleteSelected = { viewModel.deleteSelectedCloudFiles() }
                    )
                }

                // Cloud list
                CloudFilesSection(
                    cloudFiles = cloudFiles,
                    selectedIds = selectedCloudFileIds,
                    onToggleFile = { viewModel.toggleCloudFileSelection(it.id) },
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Upload cloud dialog
        if (showUploadCloudDialog) {
            SimulatedCloudUploadDialog(
                onDismiss = { showUploadCloudDialog = false },
                onUpload = { name, size ->
                    viewModel.addCloudMockFile(name, size)
                    showUploadCloudDialog = false
                }
            )
        }
    }
}

// --- Cloud Banner Title Header ---
@Composable
fun CloudBanner(
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
                    .background(AquaticWaveBlue.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = AquaticWaveBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "CLOUD ENGINE",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    ),
                    color = Color.White
                )
                Text(
                    text = "Google Drive Simulation Sandbox",
                    style = MaterialTheme.typography.bodySmall,
                    color = AquaticWaveBlue
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            GlobalProfileAvatarButton(viewModel = viewModel)
        }
    }
}

// --- OAuth / Simul Login Screen ---
@Composable
fun CloudLoginSimScreen(
    onLoginDefault: () -> Unit,
    onLoginCustom: (String) -> Unit
) {
    var inputEmail by remember { mutableStateOf("") }
    var inputToken by remember { mutableStateOf("oauth_token_client_vishwa_58284_v7") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        colors = CardDefaults.cardColors(containerColor = DeepSurfaceDark),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.VpnKey,
                contentDescription = null,
                tint = CustomFlameOrange,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Cloud Integration Simulator",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Simulate cloud-profile accounts login using mock OAuth tokens and check file states.",
                style = MaterialTheme.typography.bodySmall,
                color = TextGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = inputEmail,
                onValueChange = { inputEmail = it },
                label = { Text("Sandbox Email (e.g. user@gmail.com)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("sandbox_email_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CustomFlameOrange,
                    unfocusedBorderColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = inputToken,
                onValueChange = { inputToken = it },
                label = { Text("OAuth Sandbox Access-Token") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("sandbox_token_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CustomFlameOrange,
                    unfocusedBorderColor = Color.Gray
                ),
                trailingIcon = { Icon(Icons.Default.VerifiedUser, "sim token", tint = ForestEcoGreen) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (inputEmail.isNotBlank()) {
                        onLoginCustom(inputEmail)
                    } else {
                        onLoginDefault()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CustomFlameOrange),
                modifier = Modifier.fillMaxWidth().testTag("sandbox_login_btn")
            ) {
                Text("Log In Simulate with OAuth Sandbox", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onLoginDefault) {
                Text("Bypass with default pilot (epostvvf@gmail.com)", color = AquaticWaveBlue, fontSize = 11.sp)
            }
        }
    }
}

// --- Active Profile Card displaying switcher option ---
@Composable
fun ActiveAccountProfileCard(
    account: String,
    onSwitchClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CustomFlameOrange.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = CustomFlameOrange)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Active Cloud Sandbox",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = account,
                        fontSize = 13.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Button(
                onClick = onSwitchClick,
                colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.testTag("switch_account_trigger")
            ) {
                Text("Switch User", fontSize = 11.sp, color = Color.White)
            }
        }
    }
}

// --- CLOUD STORAGE STATUS & SYNC DASHBOARD CARD ---
@Composable
fun CloudStorageDashboardCard(
    cloudFiles: List<CloudFile>,
    viewModel: FileManagerViewModel
) {
    val isAutoSync by viewModel.isAutoSyncEnabled.collectAsStateWithLifecycle()
    val isWifiOnly by viewModel.simulateWifiOnlySync.collectAsStateWithLifecycle()

    // Calculate total used space: standard overhead (e.g. 7.96 GB) + dynamically added mock files size
    val baseOverhead = 7_960_000_000L // 7.96 GB general overhead to make simulate data look authentic
    val dynamicFilesSize = cloudFiles.sumOf { it.size }
    val totalUsedBytes = baseOverhead + dynamicFilesSize
    val totalLimitBytes = 15_000_000_000L // 15 GB Limit

    val usageProgress = (totalUsedBytes.toFloat() / totalLimitBytes.toFloat()).coerceIn(0f, 1f)
    val formattedUsed = viewModel.formatFileSize(totalUsedBytes)
    val formattedLimit = viewModel.formatFileSize(totalLimitBytes)

    var showSyncSettings by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AquaticWaveBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = AquaticWaveBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Cloud Storage Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Google Drive Sandbox",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = { showSyncSettings = !showSyncSettings },
                    modifier = Modifier.testTag("btn_sync_settings")
                ) {
                    Icon(
                        if (showSyncSettings) Icons.Default.ExpandLess else Icons.Default.Settings,
                        contentDescription = "Sync settings",
                        tint = AquaticWaveBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Bar & Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Used: $formattedUsed of $formattedLimit",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = "${(usageProgress * 100).toInt()}% Used",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AquaticWaveBlue
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { usageProgress },
                color = AquaticWaveBlue,
                trackColor = CosmicPrimary.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
            )

            // Category break down simulation
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val spaceText = if (usageProgress < 0.6f) "Secure & spacious capacity." else "Storage space is scaling up. Optimize soon!"
                Icon(Icons.Default.Info, contentDescription = null, tint = TextGray, modifier = Modifier.size(14.dp))
                Text(
                    text = "$spaceText Synced files are kept safe offline.",
                    fontSize = 11.sp,
                    color = TextGray
                )
            }

            // Sync Settings Panel (expanding section)
            AnimatedVisibility(
                visible = showSyncSettings,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .background(DeepSurfaceDark, shape = RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Simulated Sync Settings",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CustomFlameOrange
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Automatic background mirror", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Text("Auto uploads files when sync state toggled", fontSize = 10.sp, color = TextGray)
                        }
                        Switch(
                            checked = isAutoSync,
                            onCheckedChange = { viewModel.isAutoSyncEnabled.value = it },
                            modifier = Modifier.testTag("switch_auto_sync")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sync on Wi-Fi Only", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Text("Throttle syncing when on mobile network", fontSize = 10.sp, color = TextGray)
                        }
                        Switch(
                            checked = isWifiOnly,
                            onCheckedChange = { viewModel.simulateWifiOnlySync.value = it },
                            modifier = Modifier.testTag("switch_wifi_sync")
                        )
                    }
                }
            }
        }
    }
}

// --- SEMANTIC SCAN DASHBOARD CARD ---
@Composable
fun SemanticScanDashboardCard(
    isScanning: Boolean,
    progress: Float,
    onStartScan: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CustomFlameOrange.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.OfflineBolt,
                        contentDescription = null,
                        tint = CustomFlameOrange,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Semantic Drive Scan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Compute AI similarity structure scores",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Scans cloud drive structures recursively. Computes dynamic percentage correlation semantic matches representing content relevance levels.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (isScanning) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reading cloud files index mapping...", fontSize = 11.sp, color = CustomFlameOrange)
                        Text("${(progress * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CustomFlameOrange)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        color = CustomFlameOrange,
                        trackColor = CosmicPrimary.copy(alpha = 0.2f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                    )
                }
            } else {
                Button(
                    onClick = onStartScan,
                    colors = ButtonDefaults.buttonColors(containerColor = CustomFlameOrange),
                    modifier = Modifier.fillMaxWidth().testTag("btn_semantic_scan")
                ) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Initiate Core Semantic Scan", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- Cloud Search and Upload bar ---
@Composable
fun CloudSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onUploadClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search cloud files...") },
            leadingIcon = { Icon(Icons.Default.Search, "Cloud Search", tint = AquaticWaveBlue) },
            singleLine = true,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        )

        IconButton(
            onClick = onUploadClick,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(CosmicPrimary)
                .size(48.dp)
                .testTag("upload_cloud_file")
        ) {
            Icon(Icons.Default.CloudUpload, contentDescription = "Upload Cloud File", tint = Color.White)
        }
    }
}

// --- Cloud Batch Actions bar ---
@Composable
fun CloudBatchActionsBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClearSelection) {
                    Icon(Icons.Default.Close, "Cancel Selection")
                }
                Text("$selectedCount Cloud Files Selected", fontWeight = FontWeight.Bold)
            }

            IconButton(onClick = onDeleteSelected, modifier = Modifier.testTag("btn_delete_cloud_batch")) {
                Icon(Icons.Default.Delete, "Delete selected cloud", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// --- Cloud Files Table List ---
@Composable
fun CloudFilesSection(
    cloudFiles: List<CloudFile>,
    selectedIds: Set<String>,
    onToggleFile: (CloudFile) -> Unit,
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
            Text(
                text = "Simulated Drive Ledger",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (cloudFiles.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No sandbox cloud files found.", color = TextGray)
                }
            }
        }

        items(cloudFiles, key = { it.id }) { file ->
            val isSelected = selectedIds.contains(file.id)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleFile(file) }
                    .testTag("cloud_file_card_${file.id}"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleFile(file) },
                        modifier = Modifier.testTag("cloud_checkbox_${file.id}")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        Icons.Default.CloudQueue,
                        contentDescription = "Cloud File",
                        tint = AquaticWaveBlue,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${viewModel.formatFileSize(file.size)} • ${viewModel.formatDate(file.dateUpdated)}",
                            fontSize = 10.sp,
                            color = TextGray
                        )
                    }

                    // Sync state toggle icon button
                    IconButton(
                        onClick = { viewModel.toggleCloudFileSyncState(file.id) },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_sync_toggle_${file.id}")
                    ) {
                        Icon(
                            imageVector = if (file.isSynced) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                            contentDescription = "Toggle Sync State",
                            tint = if (file.isSynced) ForestEcoGreen else Color.Gray.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Score Badge showing calculated AI similarity percentages if scanning done
                    if (file.semanticScore != null) {
                        val badgeBg = when {
                            file.semanticScore >= 80 -> ForestEcoGreen.copy(alpha = 0.15f)
                            file.semanticScore >= 40 -> CustomFlameOrange.copy(alpha = 0.15f)
                            else -> Color.Gray.copy(alpha = 0.15f)
                        }
                        val badgeTxt = when {
                            file.semanticScore >= 80 -> ForestEcoGreen
                            file.semanticScore >= 40 -> CustomFlameOrange
                            else -> TextGray
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(badgeBg)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${file.semanticScore}% match",
                                fontSize = 10.sp,
                                color = badgeTxt,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Account Switcher Drop-up modal Dialog ---
@Composable
fun AccountSwitcherDialog(
    accounts: List<String>,
    selectedAccount: String?,
    onSelect: (String) -> Unit,
    onLogout: () -> Unit,
    onAddNewClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSurfaceDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("account_switcher_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Switch Simulated Account",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(14.dp))

                accounts.forEach { email ->
                    val isCurrent = email == selectedAccount
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isCurrent) CosmicPrimary else Color.Transparent
                            )
                            .clickable { onSelect(email) }
                            .padding(12.dp)
                            .testTag("profile_option_$email"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = CustomFlameOrange)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = email, color = Color.White, fontSize = 12.sp)
                        }
                        if (isCurrent) {
                            Icon(Icons.Default.Check, contentDescription = "Active", tint = ForestEcoGreen)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Divider(color = Color.Gray.copy(alpha = 0.3f))

                Spacer(modifier = Modifier.height(12.dp))

                // Actions Button List
                TextButton(onClick = onAddNewClick, modifier = Modifier.fillMaxWidth().testTag("add_account_btn")) {
                    Icon(Icons.Default.Add, "add account", tint = CustomFlameOrange)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add simulated profile using email/token", color = CustomFlameOrange, fontSize = 12.sp)
                }

                TextButton(onClick = onLogout, modifier = Modifier.fillMaxWidth().testTag("logout_sim_btn")) {
                    Icon(Icons.Default.Logout, "logout", tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log out simulated cloud Profile", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Dismiss", color = Color.White)
                }
            }
        }
    }
}

// --- OAuth / Simulated login sandbox fields ---
@Composable
fun OAuthLoginDialog(
    onDismiss: () -> Unit,
    onAddLoginSim: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSurfaceDark),
            modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("oauth_login_dialog")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Add Simulated Profile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Sandbox Email") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CustomFlameOrange),
                    modifier = Modifier.fillMaxWidth().testTag("oauth_email_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Simulated OAuth Password/Token") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CustomFlameOrange),
                    modifier = Modifier.fillMaxWidth().testTag("oauth_token_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel", color = Color.White)
                    }

                    Button(
                        onClick = {
                            val targetEmail = if (email.isBlank()) "user_${System.currentTimeMillis()}@vishwa.org" else email
                            onAddLoginSim(targetEmail, token)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CustomFlameOrange),
                        modifier = Modifier.weight(1f).testTag("btn_confirm_oauth")
                    ) {
                        Text("Log In Sim", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- simulated Cloud Upload dialog ---
@Composable
fun SimulatedCloudUploadDialog(
    onDismiss: () -> Unit,
    onUpload: (String, Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var sizeText by remember { mutableStateOf("11400000") } // ~11.4MB

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSurfaceDark),
            modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("cloud_upload_dialog")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Simulate Cloud Upload",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Cloud Filename") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CustomFlameOrange),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = sizeText,
                    onValueChange = { sizeText = it },
                    label = { Text("Size in bytes") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CustomFlameOrange),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel", color = Color.White)
                    }

                    Button(
                        onClick = {
                            val finalName = if (name.isBlank()) "cloud_upload.pdf" else name
                            val finalSize = sizeText.toLongOrNull() ?: 2450000L
                            onUpload(finalName, finalSize)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CustomFlameOrange),
                        modifier = Modifier.weight(1f).testTag("btn_confirm_upload")
                    ) {
                        Text("Upload", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
