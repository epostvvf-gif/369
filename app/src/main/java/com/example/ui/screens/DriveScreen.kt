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
import androidx.compose.ui.draw.scale
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
import com.example.data.FileEntity
import androidx.compose.ui.text.font.FontFamily

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
    var navSelection by remember { mutableStateOf(0) } // 0 = Files, 1 = Folders, 2 = Transfers & Sync
    
    val isAiSearchMode by viewModel.isAiSearchMode.collectAsStateWithLifecycle()
    val isAiSearching by viewModel.isAiSearching.collectAsStateWithLifecycle()
    val aiSearchError by viewModel.aiSearchError.collectAsStateWithLifecycle()
    val safeFiles by viewModel.safeFiles.collectAsStateWithLifecycle(initialValue = emptyList())
    val filePreview by viewModel.filePreview.collectAsStateWithLifecycle()

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
                .padding(bottom = 8.dp) // Leave height for navigation bar
        ) {
            // Screen Title Banner
            CloudBanner(viewModel = viewModel, onMenuClick = onMenuClick)

            val accountVal = activeAccount
            if (accountVal == null) {
                // If logged out - show OAuth simulated login screen
                CloudLoginSimScreen(
                    onLoginDefault = { token ->
                        viewModel.addCloudAccount("epostvvf@gmail.com")
                        viewModel.updateGoogleDriveToken(token)
                    },
                    onLoginCustom = { email, token ->
                        viewModel.addCloudAccount(email)
                        viewModel.updateGoogleDriveToken(token)
                    }
                )
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Side: Material 3 Navigation Rail
                    NavigationRail(
                        containerColor = DeepSurfaceDark,
                        contentColor = Color.White,
                        modifier = Modifier.fillMaxHeight().width(76.dp)
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        NavigationRailItem(
                            selected = navSelection == 0,
                            onClick = { navSelection = 0 },
                            icon = { Icon(Icons.Default.CloudQueue, contentDescription = "Files") },
                            label = { Text("Files", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = AquaticWaveBlue,
                                unselectedIconColor = Color.Gray,
                                selectedTextColor = AquaticWaveBlue,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = Color.White.copy(alpha = 0.08f)
                            ),
                            modifier = Modifier.testTag("rail_item_files")
                        )
                        
                        Spacer(modifier = Modifier.height(18.dp))
                        
                        NavigationRailItem(
                            selected = navSelection == 1,
                            onClick = { navSelection = 1 },
                            icon = { Icon(Icons.Default.FolderOpen, contentDescription = "Folders") },
                            label = { Text("Folders", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = AquaticWaveBlue,
                                unselectedIconColor = Color.Gray,
                                selectedTextColor = AquaticWaveBlue,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = Color.White.copy(alpha = 0.08f)
                            ),
                            modifier = Modifier.testTag("rail_item_folders")
                        )
                        
                        Spacer(modifier = Modifier.height(18.dp))
                        
                        NavigationRailItem(
                            selected = navSelection == 2,
                            onClick = { navSelection = 2 },
                            icon = { Icon(Icons.Default.Sync, contentDescription = "Transfers") },
                            label = { Text("Transfers", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = AquaticWaveBlue,
                                unselectedIconColor = Color.Gray,
                                selectedTextColor = AquaticWaveBlue,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = Color.White.copy(alpha = 0.08f)
                            ),
                            modifier = Modifier.testTag("rail_item_transfers")
                        )
                    }

                    // Right Side Content Area
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        when (navSelection) {
                            0 -> {
                                // Tab 0: Files View
                                ActiveAccountProfileCard(
                                    account = accountVal,
                                    onSwitchClick = { viewModel.showGlobalAccountSwitcher.value = true }
                                )

                                CloudSearchBar(
                                    query = searchCloudQuery,
                                    onQueryChange = { viewModel.searchCloudQuery.value = it },
                                    isAiSearchMode = isAiSearchMode,
                                    isAiSearching = isAiSearching,
                                    aiSearchError = aiSearchError,
                                    onAiSearchClick = { viewModel.performGeminiNaturalLanguageSearch(searchCloudQuery) },
                                    onClearAiSearch = { viewModel.clearAiSearch() },
                                    onUploadClick = { showUploadCloudDialog = true }
                                )

                                if (isAiSearchMode) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 6.dp),
                                        colors = CardDefaults.cardColors(containerColor = CustomFlameOrange.copy(alpha = 0.12f)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CustomFlameOrange.copy(alpha = 0.4f)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.AutoAwesome, "AI Active", tint = CustomFlameOrange, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("AI matching cloud filter active.", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Text(
                                                "Clear Results",
                                                color = CustomFlameOrange,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .clickable { viewModel.clearAiSearch() }
                                                    .testTag("clear_cloud_ai_results_btn")
                                            )
                                        }
                                    }
                                }

                                if (selectedCloudFileIds.isNotEmpty()) {
                                    CloudBatchActionsBar(
                                        selectedCount = selectedCloudFileIds.size,
                                        onClearSelection = { viewModel.selectedCloudFileIds.value = emptySet() },
                                        onDeleteSelected = { viewModel.deleteSelectedCloudFiles() }
                                    )
                                }

                                CloudFilesSection(
                                    cloudFiles = cloudFiles,
                                    selectedIds = selectedCloudFileIds,
                                    onToggleFile = { viewModel.toggleCloudFileSelection(it.id) },
                                    viewModel = viewModel,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            1 -> {
                                // Tab 1: Folders Dashboard
                                CloudFoldersDashboardSection(
                                    cloudFiles = cloudFiles,
                                    safeFiles = safeFiles,
                                    viewModel = viewModel,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            2 -> {
                                // Tab 2: Transfers & Overall Status
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    item {
                                        ActiveAccountProfileCard(
                                            account = accountVal,
                                            onSwitchClick = { viewModel.showGlobalAccountSwitcher.value = true }
                                        )
                                    }

                                    item {
                                        LiveGoogleDriveConnectionCard(viewModel = viewModel)
                                    }

                                    item {
                                        CloudStorageDashboardCard(
                                            cloudFiles = cloudFiles,
                                            viewModel = viewModel
                                        )
                                    }

                                    item {
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
                                    }

                                    item {
                                        SimulatedTransfersQueueCard(
                                            isScanning = isScanning,
                                            safeFiles = safeFiles,
                                            viewModel = viewModel
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
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
    onLoginDefault: (String) -> Unit,
    onLoginCustom: (String, String) -> Unit
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
                        onLoginCustom(inputEmail, inputToken)
                    } else {
                        onLoginDefault(inputToken)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CustomFlameOrange),
                modifier = Modifier.fillMaxWidth().testTag("sandbox_login_btn")
            ) {
                Text("Log In Simulate with OAuth Sandbox", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = { onLoginDefault("oauth_token_client_vishwa_58284_v7") }) {
                Text("Bypass with default pilot (epostvvf@gmail.com)", color = AquaticWaveBlue, fontSize = 11.sp)
            }
        }
    }
}

// --- Live Google Drive REST client connection Card & Sync Simulator ---
data class SyncableFile(
    val id: String,
    val name: String,
    val size: String,
    val sizeBytes: Long,
    val progress: Float = 0f,
    val status: String = "Queued" // "Queued", "Syncing", "Synced", "Failed"
)

@Composable
fun LiveGoogleDriveConnectionCard(
    viewModel: FileManagerViewModel
) {
    val accessToken by viewModel.googleDriveAccessToken.collectAsStateWithLifecycle()
    val isFetching by viewModel.isFetchingGoogleDrive.collectAsStateWithLifecycle()
    val connectionError by viewModel.googleDriveConnectionError.collectAsStateWithLifecycle()

    var tokenInput by remember { mutableStateOf(accessToken) }
    var isEditing by remember { mutableStateOf(false) }

    // Simulator Interactive States
    var isSyncing by remember { mutableStateOf(false) }
    var globalProgress by remember { mutableStateOf(0f) }
    var currentSpeedText by remember { mutableStateOf("0 KB/s") }
    var totalBytesTransferred by remember { mutableStateOf(0L) }
    var networkPreset by remember { mutableStateOf("Wi-Fi") } // "LTE", "Wi-Fi", "Fiber", "Offline"
    var showNetworkErrorSim by remember { mutableStateOf(false) }
    var syncErrorOccurred by remember { mutableStateOf(false) }
    var syncErrorMessage by remember { mutableStateOf("") }

    val syncQueue = remember {
        mutableStateListOf(
            SyncableFile("s1", "vishwa_brochure_m3.pdf", "5.2 MB", 5452595L),
            SyncableFile("s2", "donor_list_2026.xlsx", "1.3 MB", 1363148L),
            SyncableFile("s3", "trustee_resolutions_signed.pdf", "0.8 MB", 838860L),
            SyncableFile("s4", "foundation_chants_chords.wav", "15.4 MB", 16148070L)
        )
    }

    LaunchedEffect(accessToken) {
        if (tokenInput != accessToken) {
            tokenInput = accessToken
        }
    }

    // Interactive Sync Simulation Thread loop
    LaunchedEffect(isSyncing, networkPreset, showNetworkErrorSim) {
        if (!isSyncing) return@LaunchedEffect

        while (isSyncing) {
            // Find next un-synced file or failed file
            val activeIndex = syncQueue.indexOfFirst { it.status == "Queued" || it.status == "Syncing" || it.status == "Failed" }
            if (activeIndex == -1) {
                // Done with all syncing!
                isSyncing = false
                currentSpeedText = "0 KB/s"
                break
            }

            val activeFile = syncQueue[activeIndex]
            if (activeFile.status != "Syncing") {
                syncQueue[activeIndex] = activeFile.copy(status = "Syncing")
            }

            // Speed estimation
            val speedBytesPerSec = when (networkPreset) {
                "LTE" -> 350_000L // 350 KB/s
                "Wi-Fi" -> 2_800_000L // 2.8 MB/s
                "Fiber" -> 16_000_000L // 16.0 MB/s
                else -> 0L // Offline
            }

            if (networkPreset == "Offline") {
                currentSpeedText = "Offline (Suspended)"
                syncQueue[activeIndex] = activeFile.copy(status = "Queued")
                kotlinx.coroutines.delay(600)
                continue
            }

            currentSpeedText = when (networkPreset) {
                "LTE" -> "350 KB/s (LTE/Cellular)"
                "Wi-Fi" -> "2.8 MB/s (Standard Wi-Fi)"
                "Fiber" -> "16.0 MB/s (High Speed Fiber)"
                else -> "Offline (Suspended)"
            }

            // Calculate the delta progress step per 150ms tick
            // ratio = bytes_transferred_in_tick / total_file_bytes
            val incrementRatio = (speedBytesPerSec / 6.6f) / activeFile.sizeBytes.toFloat()
            val nextProgress = (activeFile.progress + incrementRatio).coerceAtMost(1.0f)

            // Dynamic Failure Injection check (halves midway at 50% global progress)
            if (showNetworkErrorSim && globalProgress >= 0.45f) {
                syncErrorOccurred = true
                syncErrorMessage = "Google Drive tunnel handshake timeout. SSL proxy connection lost."
                syncQueue[activeIndex] = activeFile.copy(status = "Failed")
                isSyncing = false
                currentSpeedText = "0 KB/s"
                break
            }

            kotlinx.coroutines.delay(150)

            val statusNext = if (nextProgress >= 1.0f) "Synced" else "Syncing"
            syncQueue[activeIndex] = activeFile.copy(progress = nextProgress, status = statusNext)

            // Recompute total global metadata progress
            val totalBytesSum = syncQueue.sumOf { it.sizeBytes }
            val completedBytesSum = syncQueue.sumOf { (it.progress * it.sizeBytes).toLong() }
            totalBytesTransferred = completedBytesSum
            globalProgress = (completedBytesSum.toFloat() / totalBytesSum.toFloat()).coerceIn(0f, 1f)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("drive_connectivity_box"),
        colors = CardDefaults.cardColors(containerColor = DeepSurfaceDark),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Icon + Title + Connectivity Pill Badge
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
                            .background(
                                if (isSyncing) ForestEcoGreen.copy(alpha = 0.15f)
                                else Color.White.copy(alpha = 0.05f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSyncing) Icons.Default.CloudSync else Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = if (isSyncing) ForestEcoGreen else AquaticWaveBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Google Drive DriveLink",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (accessToken.isNotBlank()) "OAuth Access Token authenticated" else "Sandbox emulation",
                            fontSize = 10.sp,
                            color = TextGray
                        )
                    }
                }

                // Dynamic Status Pill
                val statusBg = when {
                    syncErrorOccurred -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                    isSyncing -> ForestEcoGreen.copy(alpha = 0.15f)
                    networkPreset == "Offline" -> Color.Gray.copy(alpha = 0.15f)
                    else -> AquaticWaveBlue.copy(alpha = 0.15f)
                }
                val statusText = when {
                    syncErrorOccurred -> "FAILED"
                    isSyncing -> "SYNCING"
                    networkPreset == "Offline" -> "OFFLINE"
                    else -> "READY"
                }
                val statusColor = when {
                    syncErrorOccurred -> MaterialTheme.colorScheme.error
                    isSyncing -> ForestEcoGreen
                    networkPreset == "Offline" -> Color.Gray
                    else -> AquaticWaveBlue
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sub-Section 1: Connection Token configuration collapsible block & quick controls
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = ForestEcoGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (viewModel.selectedCloudAccount.value != null) "${viewModel.selectedCloudAccount.value}" else "Deauthed",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { isEditing = !isEditing },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = if (isEditing) "Hide Credentials" else "Auth Details",
                            fontSize = 11.sp,
                            color = AquaticWaveBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (isEditing) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        placeholder = { Text("ya29.a0Ac...", fontSize = 11.sp, color = Color.Gray) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("live_token_input"),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AquaticWaveBlue,
                            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.3f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.updateGoogleDriveToken(tokenInput)
                            isEditing = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AquaticWaveBlue),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("Save Token", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Sub-Section 2: Simulator Action Controls (Speed, Error injection, Play/Pause)
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "SIMULATOR CONTROLLER PROFILE",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = TextGray
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Speed Presets Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    Triple("LTE", Icons.Default.SignalCellularAlt, "Cellular"),
                    Triple("Wi-Fi", Icons.Default.Wifi, "Wi-Fi"),
                    Triple("Fiber", Icons.Default.Bolt, "Fiber"),
                    Triple("Offline", Icons.Default.CloudOff, "Off")
                ).forEach { (preset, icon, label) ->
                    val isSelected = networkPreset == preset
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) AquaticWaveBlue.copy(alpha = 0.15f)
                                else Color.White.copy(alpha = 0.04f)
                            )
                            .clickable { networkPreset = preset }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) AquaticWaveBlue else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                                color = if (isSelected) Color.White else TextGray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Error injection toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column {
                    Text(
                        text = "Inject Connection Flaw",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Simulates handshakes failure midway (45% progress)",
                        fontSize = 9.sp,
                        color = TextGray
                    )
                }
                Switch(
                    checked = showNetworkErrorSim,
                    onCheckedChange = { showNetworkErrorSim = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CustomFlameOrange,
                        checkedTrackColor = CustomFlameOrange.copy(alpha = 0.3f),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier.scale(0.8f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Play / Pause / Reset Simulator Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isSyncing) {
                    Button(
                        onClick = {
                            if (globalProgress >= 1f || syncErrorOccurred) {
                                // Full reset of simulation parameters
                                syncErrorOccurred = false
                                syncErrorMessage = ""
                                globalProgress = 0f
                                totalBytesTransferred = 0L
                                syncQueue.indices.forEach { i ->
                                    syncQueue[i] = syncQueue[i].copy(progress = 0f, status = "Queued")
                                }
                            }
                            isSyncing = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AquaticWaveBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (globalProgress > 0f) "Resume Synchronization" else "Start Synchronization",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { isSyncing = false },
                        colors = ButtonDefaults.buttonColors(containerColor = CustomFlameOrange),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Pause, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pause Syncing", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        isSyncing = false
                        globalProgress = 0f
                        totalBytesTransferred = 0L
                        syncErrorOccurred = false
                        syncErrorMessage = ""
                        syncQueue.indices.forEach { i ->
                            syncQueue[i] = syncQueue[i].copy(progress = 0f, status = "Queued")
                        }
                    },
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear Status", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Sync Failure Alerts Banner
            if (syncErrorOccurred) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Sync error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Synchronization Blocked",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = syncErrorMessage,
                                fontSize = 10.sp,
                                color = TextGray
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Turn off 'Inject Connection Flaw' and click Resume to clear SSL blockage.",
                                fontSize = 9.sp,
                                color = CustomFlameOrange,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    showNetworkErrorSim = false
                                    syncErrorOccurred = false
                                    isSyncing = true
                                }
                            )
                        }
                    }
                }
            }

            // Sub-Section 3: Progress Visualizer Panel (Circular and Linear Metrics)
            if (globalProgress > 0f || isSyncing) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Circular Progress Dial
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(72.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { globalProgress },
                            color = if (syncErrorOccurred) MaterialTheme.colorScheme.error else ForestEcoGreen,
                            trackColor = Color.White.copy(alpha = 0.08f),
                            strokeWidth = 5.dp,
                            modifier = Modifier.fillMaxSize()
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${(globalProgress * 100).toInt()}%",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "global",
                                fontSize = 8.sp,
                                color = TextGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Global Speed details and meta metrics
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isSyncing) "Transfer Speed: $currentSpeedText" else "Transfer Interrupted",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSyncing) ForestEcoGreen else CustomFlameOrange
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        val totalSizeFormatted = "22.7 MB"
                        val transferredFormatted = String.format(java.util.Locale.US, "%.1f MB", totalBytesTransferred / 1_048_576f)
                        Text(
                            text = "Transferred: $transferredFormatted / $totalSizeFormatted",
                            fontSize = 10.sp,
                            color = TextGray
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Linear progress status representation
                        LinearProgressIndicator(
                            progress = { globalProgress },
                            color = if (syncErrorOccurred) MaterialTheme.colorScheme.error else ForestEcoGreen,
                            trackColor = Color.White.copy(alpha = 0.08f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    }
                }
            }

            // Sub-Section 4: Multi-File Backup List Queue Status Items
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "SYNCHRONIZATION QUEUE REGISTRY",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = TextGray
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                syncQueue.forEach { fileItem ->
                    val isCurrentSyncing = fileItem.status == "Syncing"
                    val itemBg = if (isCurrentSyncing) Color.White.copy(alpha = 0.04f) else Color.Transparent

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(itemBg)
                            .padding(vertical = 4.dp, horizontal = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1.5f)
                            ) {
                                Icon(
                                    imageVector = when (fileItem.status) {
                                        "Synced" -> Icons.Default.CloudDone
                                        "Syncing" -> Icons.Default.CloudUpload
                                        "Failed" -> Icons.Default.Error
                                        else -> Icons.Default.QueryBuilder
                                    },
                                    contentDescription = fileItem.status,
                                    tint = when (fileItem.status) {
                                        "Synced" -> ForestEcoGreen
                                        "Syncing" -> AquaticWaveBlue
                                        "Failed" -> MaterialTheme.colorScheme.error
                                        else -> Color.Gray
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = fileItem.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrentSyncing) Color.White else TextGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = fileItem.size,
                                    fontSize = 10.sp,
                                    color = TextGray,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                // Row Badge Status text
                                Text(
                                    text = fileItem.status,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = when (fileItem.status) {
                                        "Synced" -> ForestEcoGreen
                                        "Syncing" -> AquaticWaveBlue
                                        "Failed" -> MaterialTheme.colorScheme.error
                                        else -> Color.Gray
                                    }
                                )
                            }
                        }

                        if (isCurrentSyncing) {
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { fileItem.progress },
                                color = AquaticWaveBlue,
                                trackColor = Color.White.copy(alpha = 0.05f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .clip(RoundedCornerShape(1.dp))
                            )
                        }
                    }
                }
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

                    val isDriveSyncEnabled by viewModel.isGoogleDriveSyncEnabled.collectAsStateWithLifecycle()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Live Google Drive Sync", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Text("Actively sync remote files. Disable to save bandwidth.", fontSize = 10.sp, color = TextGray)
                        }
                        Switch(
                            checked = isDriveSyncEnabled,
                            onCheckedChange = { viewModel.updateGoogleDriveSyncEnabled(it) },
                            modifier = Modifier.testTag("switch_drive_sync_enabled")
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
    isAiSearchMode: Boolean,
    isAiSearching: Boolean,
    aiSearchError: String?,
    onAiSearchClick: () -> Unit,
    onClearAiSearch: () -> Unit,
    onUploadClick: () -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search cloud files...") },
                leadingIcon = { Icon(Icons.Default.Search, "Cloud Search", tint = AquaticWaveBlue) },
                trailingIcon = {
                    if (query.isNotEmpty() || isAiSearchMode) {
                        IconButton(onClick = {
                            if (isAiSearchMode) {
                                onClearAiSearch()
                            } else {
                                onQueryChange("")
                            }
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = Color.Gray)
                        }
                    }
                },
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

        // Gemini AI Natural Language Sparkle option block
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isAiSearching) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = CustomFlameOrange)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gemini is searching content...", fontSize = 11.sp, color = CustomFlameOrange, fontWeight = FontWeight.Bold)
                }
            } else {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onAiSearchClick() }
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Gemini Sparkle Search",
                        tint = CustomFlameOrange,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Query Cloud via Content & Description (AI)",
                        color = CustomFlameOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (query.isNotBlank() && !isAiSearching) {
                Text(
                    text = "Submit AI Query",
                    color = CustomFlameOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onAiSearchClick() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .testTag("submit_cloud_ai_search_btn")
                )
            }
        }

        if (aiSearchError != null) {
            Text(
                text = aiSearchError,
                color = MaterialTheme.colorScheme.error,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
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
                    .clickable { viewModel.showCloudFilePreview(file) }
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

// ====================== DYNAMIC SIMULATED FOLDERS & TRANSFERS QUEUES ======================

@Composable
fun CloudFoldersDashboardSection(
    cloudFiles: List<CloudFile>,
    safeFiles: List<com.example.data.FileEntity>?,
    viewModel: FileManagerViewModel,
    modifier: Modifier = Modifier
) {
    val docFiles = cloudFiles.filter { file ->
        val lower = file.name.lowercase()
        lower.endsWith(".pdf") || lower.endsWith(".xlsx") || lower.endsWith(".xls") || lower.endsWith(".doc") || lower.endsWith(".docx") || lower.endsWith(".txt")
    }
    val mediaFiles = cloudFiles.filter { file ->
        val lower = file.name.lowercase()
        lower.endsWith(".wav") || lower.endsWith(".mp3") || lower.endsWith(".mp4") || lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
    }
    val safeSyncCount = safeFiles?.size ?: 0
    val safeSyncSize = safeFiles?.sumOf { it.size } ?: 0L

    var expandedFolder by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = AquaticWaveBlue,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "Directory Explorer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "Encrypted partitions on Google Drive sandbox",
                        fontSize = 11.sp,
                        color = TextGray
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Folder 1: Personal Documents
        item {
            FolderCard(
                name = "Personal Documents",
                itemCount = docFiles.size,
                totalSize = docFiles.sumOf { it.size },
                description = "Seeded pdfs, ledger excels, and trustee resolutions.",
                icon = Icons.Default.Description,
                iconColor = CustomFlameOrange,
                syncStatus = "Fully Synced & Verified",
                syncColor = ForestEcoGreen,
                isExpanded = expandedFolder == "docs",
                onToggleExpand = { expandedFolder = if (expandedFolder == "docs") null else "docs" },
                files = docFiles,
                viewModel = viewModel
            )
        }

        // Folder 2: Multimedia & Audio Assets
        item {
            FolderCard(
                name = "Multimedia & Audio Assets",
                itemCount = mediaFiles.size,
                totalSize = mediaFiles.sumOf { it.size },
                description = "Theme chants WAV sound files, branding snapshots.",
                icon = Icons.Default.PlayCircle,
                iconColor = AquaticWaveBlue,
                syncStatus = "Sync Enabled (Wi-Fi Only)",
                syncColor = AquaticWaveBlue,
                isExpanded = expandedFolder == "media",
                onToggleExpand = { expandedFolder = if (expandedFolder == "media") null else "media" },
                files = mediaFiles,
                viewModel = viewModel
            )
        }

        // Folder 3: Secure Vault Sync Mirrors
        item {
            FolderCard(
                name = "Secure Vault Mirrors",
                itemCount = safeSyncCount,
                totalSize = safeSyncSize,
                description = "Decryption protection offline exports stored in private cloud.",
                icon = Icons.Default.Lock,
                iconColor = ForestEcoGreen,
                syncStatus = if (safeSyncCount > 0) "Auto-Cloud Sync Active" else "No safe files in cloud yet",
                syncColor = if (safeSyncCount > 0) ForestEcoGreen else Color.Gray,
                isExpanded = expandedFolder == "vault",
                onToggleExpand = { expandedFolder = if (expandedFolder == "vault") null else "vault" },
                files = emptyList(),
                viewModel = viewModel,
                customSafeMessage = "Encrypted binary safe files. Decrypt locally with Vault PIN."
            )
        }

        // Folder 4: Other Root Items
        val otherFiles = cloudFiles.filter { it !in docFiles && it !in mediaFiles }
        if (otherFiles.isNotEmpty()) {
            item {
                FolderCard(
                    name = "Other Sandbox Objects",
                    itemCount = otherFiles.size,
                    totalSize = otherFiles.sumOf { it.size },
                    description = "Miscellaneous unclassified developer test objects.",
                    icon = Icons.Default.Extension,
                    iconColor = Color.Magenta,
                    syncStatus = "Standalone Sandbox Status",
                    syncColor = Color.Gray,
                    isExpanded = expandedFolder == "other",
                    onToggleExpand = { expandedFolder = if (expandedFolder == "other") null else "other" },
                    files = otherFiles,
                    viewModel = viewModel
                )
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun FolderCard(
    name: String,
    itemCount: Int,
    totalSize: Long,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    syncStatus: String,
    syncColor: Color,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    files: List<CloudFile>,
    viewModel: FileManagerViewModel,
    customSafeMessage: String? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() }
            .testTag("folder_card_${name.lowercase().replace(" ", "_").replace("&", "and")}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                            .background(iconColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("$itemCount items • ${viewModel.formatFileSize(totalSize)}", fontSize = 10.sp, color = TextGray)
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand folder",
                    tint = TextGray,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(description, fontSize = 11.sp, color = TextGray)

            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(syncColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(syncStatus, fontSize = 10.sp, color = syncColor, fontWeight = FontWeight.Bold)
            }

            // Expanding child contents
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .background(DeepSurfaceDark, shape = RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("FOLDER DIRECTORY CONTENTS:", fontSize = 9.sp, color = iconColor, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    HorizontalDivider(color = Color.White.copy(alpha = 0.10f), modifier = Modifier.padding(vertical = 4.dp))

                    if (customSafeMessage != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = ForestEcoGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(customSafeMessage, fontSize = 11.sp, color = TextGray)
                        }
                    } else if (files.isEmpty()) {
                        Text("No matching items in this directory folder.", fontSize = 11.sp, color = Color.Gray)
                    } else {
                        files.forEach { file ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.showCloudFilePreview(file) }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = if (file.name.endsWith(".pdf")) Icons.Default.PictureAsPdf else Icons.Default.InsertDriveFile,
                                        contentDescription = null,
                                        tint = iconColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = file.name,
                                        fontSize = 11.sp,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(viewModel.formatFileSize(file.size), fontSize = 10.sp, color = TextGray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimulatedTransfersQueueCard(
    isScanning: Boolean,
    safeFiles: List<com.example.data.FileEntity>?,
    viewModel: FileManagerViewModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
                        .background(ForestEcoGreen.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = ForestEcoGreen, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Transfer Queues & Core Log",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Active secure synchronized tunnels",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Active Scan / Backup
            if (isScanning) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DeepSurfaceDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = CustomFlameOrange, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Recursive Directory Evaluation...", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Recalculating hash keys and semantic score grids...", fontSize = 10.sp, color = TextGray)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Simulated Sync Transfers
            Text("Simulated Sync Active Queues (Tunnels):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ForestEcoGreen, letterSpacing = 0.5.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Task 1: Safe Backup
                val hasSafes = (safeFiles?.size ?: 0) > 0
                val safeText = if (hasSafes) "Backup: Secure_Vault_Backup_${safeFiles?.size ?: 1}.enc" else "Idle (Secure Sync Connection)"
                val safeSub = if (hasSafes) "Enforced PIN Decryption Protection • Ready" else "Waiting for new local files to sync"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeepSurfaceDark, shape = RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (hasSafes) Icons.Default.Lock else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (hasSafes) ForestEcoGreen else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(safeText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Text(safeSub, fontSize = 9.sp, color = TextGray)
                        }
                    }
                    if (hasSafes) {
                        Text("100%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ForestEcoGreen)
                    }
                }

                // Task 2: Live Download simulation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeepSurfaceDark, shape = RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = AquaticWaveBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Automatic Restores Simulator", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Text("Mirroring downloads rate: 4.8 MB/s", fontSize = 9.sp, color = TextGray)
                        }
                    }
                    Icon(Icons.Default.CloudDone, contentDescription = "Active", tint = AquaticWaveBlue, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

// --- Account Switcher Bottom Sheet mirroring Google Drive account switching behavior ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSwitcherBottomSheet(
    accounts: List<String>,
    selectedAccount: String?,
    onSelect: (String) -> Unit,
    onLogout: () -> Unit,
    onAddNewClick: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DeepSurfaceDark,
        contentColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray.copy(alpha = 0.5f)) },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.testTag("account_switcher_dialog")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp, top = 8.dp)
        ) {
            // Google Account Branding Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "G",
                    color = AquaticWaveBlue,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp
                )
                Text(
                    text = "o",
                    color = CustomFlameOrange,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp
                )
                Text(
                    text = "o",
                    color = Color.Yellow,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp
                )
                Text(
                    text = "g",
                    color = AquaticWaveBlue,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp
                )
                Text(
                    text = "l",
                    color = ForestEcoGreen,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp
                )
                Text(
                    text = "e",
                    color = CustomFlameOrange,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Account",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Accounts List styled as Google Drive profile cards
            accounts.forEach { email ->
                val isCurrent = email == selectedAccount
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isCurrent) Color.White.copy(alpha = 0.08f) else Color.Transparent
                        )
                        .clickable { onSelect(email) }
                        .padding(14.dp)
                        .testTag("profile_option_$email"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCurrent) CustomFlameOrange.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = email.firstOrNull()?.uppercaseChar()?.toString() ?: "U",
                            color = if (isCurrent) CustomFlameOrange else Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = email,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                        )
                        if (isCurrent) {
                            Text(
                                text = "Currently active drive",
                                color = ForestEcoGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (isCurrent) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Active Checkmark",
                            tint = ForestEcoGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color.White.copy(alpha = 0.12f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Actions list mirroring Google Drive options
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onAddNewClick() }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("add_account_btn"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = CustomFlameOrange,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Add another account",
                    color = CustomFlameOrange,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onLogout() }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("logout_sim_btn"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Logout of all accounts",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
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
