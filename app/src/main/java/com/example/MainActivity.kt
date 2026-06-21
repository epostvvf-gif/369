package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AIScreen
import com.example.ui.screens.DriveScreen
import com.example.ui.screens.MainScreen
import com.example.ui.screens.AIChatDrawer
import com.example.ui.screens.JunkCleanerScreen
import com.example.ui.screens.StorageAnalyticsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.SyncSettingsScreen
import com.example.ui.screens.AccountSwitcherBottomSheet
import com.example.ui.screens.OAuthLoginDialog
import com.example.ui.theme.*
import com.example.viewmodel.FileManagerViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContainer()
            }
        }
    }
}

@Composable
fun MainAppContainer() {
    val viewModel: FileManagerViewModel = viewModel()
    val context = LocalContext.current

    // Launcher for older Android storage permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        viewModel.scanRealFilesystem()
    }
    
    // Launcher for all files access on Android 11+
    val allFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        viewModel.scanRealFilesystem()
    }

    LaunchedEffect(Unit) {
        viewModel.scanRealFilesystem()
    }

    var showSplashScreen by remember { mutableStateOf(true) }
    var selectedIndex by remember { mutableStateOf(0) }
    val isChatDrawerOpen by viewModel.isChatDrawerOpen.collectAsStateWithLifecycle()
    
    val activeAccount by viewModel.selectedCloudAccount.collectAsStateWithLifecycle()
    val activeAccountName by viewModel.selectedCloudAccountName.collectAsStateWithLifecycle()
    val accounts by viewModel.cloudAccounts.collectAsStateWithLifecycle()
    val showAccountDialog by viewModel.showGlobalAccountSwitcher.collectAsStateWithLifecycle()
    val showAddAccountDialog by viewModel.showGlobalAddAccount.collectAsStateWithLifecycle()
    val showApiKeyPromptDialogForScan by viewModel.showApiKeyPromptDialogForScan.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    if (showApiKeyPromptDialogForScan) {
        ApiKeyPromptDialog(
            onConfirm = { keyText ->
                viewModel.geminiApiKey.value = keyText
                viewModel.showApiKeyPromptDialogForScan.value = false
                viewModel.startSemanticScan()
            },
            onDismiss = {
                viewModel.showApiKeyPromptDialogForScan.value = false
            }
        )
    }

    if (showAccountDialog) {
        AccountSwitcherBottomSheet(
            accounts = accounts,
            selectedAccount = activeAccount,
            onSelect = { email ->
                viewModel.selectCloudAccount(email)
                viewModel.showGlobalAccountSwitcher.value = false
            },
            onLogout = {
                viewModel.logoutFromCloudAccount()
                viewModel.showGlobalAccountSwitcher.value = false
            },
            onAddNewClick = {
                viewModel.showGlobalAddAccount.value = true
                viewModel.showGlobalAccountSwitcher.value = false
            },
            onDismiss = { viewModel.showGlobalAccountSwitcher.value = false }
        )
    }

    if (showAddAccountDialog) {
        OAuthLoginDialog(
            onDismiss = { viewModel.showGlobalAddAccount.value = false },
            onAddLoginSim = { name, email, token ->
                viewModel.addCloudAccount(email, name)
                viewModel.showGlobalAddAccount.value = false
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth >= 600.dp

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = !isWide,
            drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = DeepSurfaceDark,
                drawerContentColor = Color.White,
                modifier = Modifier
                    .width(310.dp)
                    .fillMaxHeight(),
                windowInsets = WindowInsets.navigationBars
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DeepSurfaceDark)
                ) {
                    // Modern Drawer Header with Logo & Account info
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        CosmicPrimary,
                                        DeepSurfaceDark
                                    )
                                )
                            )
                            .padding(horizontal = 24.dp, vertical = 28.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
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
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "VISHVA SPACE",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Core Storage Platform",
                                        fontSize = 11.sp,
                                        color = TextGray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Interactive User Account details
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.04f))
                                    .clickable { viewModel.showGlobalAccountSwitcher.value = true }
                                    .padding(10.dp)
                                    .testTag("drawer_switch_account_row")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (activeAccount != null) CustomFlameOrange.copy(alpha = 0.15f)
                                            else Color.White.copy(alpha = 0.10f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (activeAccount != null) {
                                        val firstLetter = activeAccount?.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
                                        Text(
                                            text = firstLetter,
                                            color = CustomFlameOrange,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = activeAccountName ?: activeAccount ?: "No Account / Guest",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (activeAccount != null && activeAccountName != null) {
                                        Text(
                                            text = activeAccount ?: "",
                                            fontSize = 9.sp,
                                            color = TextGray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (activeAccount != null) ForestEcoGreen else Color.Gray)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = if (activeAccount != null) "Active Workspace" else "Click to Switch / Login",
                                            fontSize = 9.sp,
                                            color = if (activeAccount != null) ForestEcoGreen else TextGray,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Drawer Navigation Items List
                    ScrollViewDrawerItemsList(
                        selectedIndex = selectedIndex,
                        onSelect = { index ->
                            selectedIndex = index
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {
                if (isWide) {
                    NavigationRail(
                        containerColor = DeepSurfaceDark,
                        header = {
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 16.dp)
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(androidx.compose.ui.graphics.Brush.sweepGradient(listOf(CustomFlameOrange, AquaticWaveBlue, ForestEcoGreen, CustomFlameOrange)))
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
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxHeight()
                            .testTag("app_navigation_rail")
                    ) {
                        Spacer(modifier = Modifier.weight(0.1f))
                        
                        NavigationRailItem(
                            selected = selectedIndex == 0,
                            onClick = { selectedIndex = 0 },
                            icon = { Icon(Icons.Default.Storage, contentDescription = "Local Files") },
                            label = { Text("Local Files", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = CustomFlameOrange,
                                selectedTextColor = Color.White,
                                indicatorColor = CosmicPrimary,
                                unselectedIconColor = TextGray,
                                unselectedTextColor = TextGray
                            ),
                            modifier = Modifier.testTag("rail_item_local_files")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        NavigationRailItem(
                            selected = selectedIndex == 1,
                            onClick = { selectedIndex = 1 },
                            icon = { Icon(Icons.Default.CloudQueue, contentDescription = "Cloud Drive") },
                            label = { Text("Cloud Drive", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = AquaticWaveBlue,
                                selectedTextColor = Color.White,
                                indicatorColor = CosmicPrimary,
                                unselectedIconColor = TextGray,
                                unselectedTextColor = TextGray
                            ),
                            modifier = Modifier.testTag("rail_item_cloud_files")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        NavigationRailItem(
                            selected = selectedIndex == 2,
                            onClick = { selectedIndex = 2 },
                            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = "Junk Cleaner") },
                            label = { Text("Junk Cleaner", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = CustomFlameOrange,
                                selectedTextColor = Color.White,
                                indicatorColor = CosmicPrimary,
                                unselectedIconColor = TextGray,
                                unselectedTextColor = TextGray
                            ),
                            modifier = Modifier.testTag("rail_item_junk_cleaner")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        NavigationRailItem(
                            selected = selectedIndex == 3,
                            onClick = { selectedIndex = 3 },
                            icon = { Icon(Icons.Default.SmartToy, contentDescription = "AI Assistant") },
                            label = { Text("AI Assistant", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = CustomFlameOrange,
                                selectedTextColor = Color.White,
                                indicatorColor = CosmicPrimary,
                                unselectedIconColor = TextGray,
                                unselectedTextColor = TextGray
                            ),
                            modifier = Modifier.testTag("rail_item_ai_assistant")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        NavigationRailItem(
                            selected = selectedIndex == 4,
                            onClick = { selectedIndex = 4 },
                            icon = { Icon(Icons.Default.PieChart, contentDescription = "Analytics") },
                            label = { Text("Analytics", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = ForestEcoGreen,
                                selectedTextColor = Color.White,
                                indicatorColor = CosmicPrimary,
                                unselectedIconColor = TextGray,
                                unselectedTextColor = TextGray
                            ),
                            modifier = Modifier.testTag("rail_item_storage_analytics")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        NavigationRailItem(
                            selected = selectedIndex == 5,
                            onClick = { selectedIndex = 5 },
                            icon = { Icon(Icons.Default.Sync, contentDescription = "Sync Settings") },
                            label = { Text("Sync Set", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = CustomFlameOrange,
                                selectedTextColor = Color.White,
                                indicatorColor = CosmicPrimary,
                                unselectedIconColor = TextGray,
                                unselectedTextColor = TextGray
                            ),
                            modifier = Modifier.testTag("rail_item_sync_settings")
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        GlobalProfileAvatarButton(viewModel = viewModel)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                floatingActionButton = {
                    if (selectedIndex != 3) { // Hide when already in full-tab AIScreen
                        FloatingActionButton(
                            onClick = { viewModel.isChatDrawerOpen.value = !isChatDrawerOpen },
                            containerColor = CustomFlameOrange,
                            contentColor = Color.White,
                            modifier = Modifier
                                .padding(bottom = 12.dp)
                                .testTag("fab_ai_chat_drawer"),
                            elevation = FloatingActionButtonDefaults.elevation(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = "Quick AI Assistant Chat Drawer"
                            )
                        }
                    }
                }
            ) { innerPadding ->
                // Animated transition between screens
                AnimatedContent(
                    targetState = selectedIndex,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    label = "screen_navigation_content"
                ) { targetIndex ->
                    when (targetIndex) {
                        0 -> MainScreen(viewModel = viewModel, onMenuClick = { scope.launch { drawerState.open() } })
                        1 -> DriveScreen(viewModel = viewModel, onMenuClick = { scope.launch { drawerState.open() } })
                        2 -> JunkCleanerScreen(viewModel = viewModel, onMenuClick = { scope.launch { drawerState.open() } })
                        3 -> AIScreen(viewModel = viewModel, onMenuClick = { scope.launch { drawerState.open() } })
                        4 -> StorageAnalyticsScreen(viewModel = viewModel, onMenuClick = { scope.launch { drawerState.open() } })
                        5 -> SyncSettingsScreen(viewModel = viewModel, onMenuClick = { scope.launch { drawerState.open() } })
                    }
                }
            }
                }
            }

            // Custom Slide-in Drawer from Right with Dark Backdrop
            if (isChatDrawerOpen) {
                // Semi-transparent backdrop to focus interaction onto the drawer
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { viewModel.isChatDrawerOpen.value = false }
                )
            }

            AnimatedVisibility(
                visible = isChatDrawerOpen,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it }),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
            ) {
                AIChatDrawer(
                    viewModel = viewModel,
                    onClose = { viewModel.isChatDrawerOpen.value = false }
                )
            }
        }
        
        AnimatedVisibility(
            visible = showSplashScreen,
            enter = fadeIn(),
            exit = fadeOut(animationSpec = tween(durationMillis = 800))
        ) {
            SplashScreen(onSplashFinished = { showSplashScreen = false })
        }
    }
}
}
}

@Composable
fun ScrollViewDrawerItemsList(
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Section 1: FILE LOGISTICS
        Text(
            text = "FILE LOGISTICS ENGINE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = CustomFlameOrange,
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 6.dp)
        )

        NavigationDrawerItem(
            label = { Text("Local File Browser", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            selected = selectedIndex == 0,
            onClick = { onSelect(0) },
            icon = { Icon(Icons.Default.Storage, contentDescription = null) },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = CosmicPrimary,
                selectedIconColor = CustomFlameOrange,
                selectedTextColor = Color.White,
                unselectedContainerColor = Color.Transparent,
                unselectedIconColor = TextGray,
                unselectedTextColor = TextGray
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("drawer_item_local_files")
        )

        NavigationDrawerItem(
            label = { Text("Cloud Sync Drive", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            selected = selectedIndex == 1,
            onClick = { onSelect(1) },
            icon = { Icon(Icons.Default.CloudQueue, contentDescription = null) },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = CosmicPrimary,
                selectedIconColor = AquaticWaveBlue,
                selectedTextColor = Color.White,
                unselectedContainerColor = Color.Transparent,
                unselectedIconColor = TextGray,
                unselectedTextColor = TextGray
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("drawer_item_cloud_files")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Section 2: OPTIMIZATION & CARE
        Text(
            text = "CARE & OPTIMIZATION",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = AquaticWaveBlue,
            modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 6.dp)
        )

        NavigationDrawerItem(
            label = { Text("System Junk Cleaner", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            selected = selectedIndex == 2,
            onClick = { onSelect(2) },
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = CosmicPrimary,
                selectedIconColor = CustomFlameOrange,
                selectedTextColor = Color.White,
                unselectedContainerColor = Color.Transparent,
                unselectedIconColor = TextGray,
                unselectedTextColor = TextGray
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("drawer_item_junk_cleaner")
        )

        Spacer(modifier = Modifier.height(12.dp))

        NavigationDrawerItem(
            label = { Text("Storage Analytics", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            selected = selectedIndex == 4,
            onClick = { onSelect(4) },
            icon = { Icon(Icons.Default.PieChart, contentDescription = null) },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = CosmicPrimary,
                selectedIconColor = ForestEcoGreen,
                selectedTextColor = Color.White,
                unselectedContainerColor = Color.Transparent,
                unselectedIconColor = TextGray,
                unselectedTextColor = TextGray
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("drawer_item_storage_analytics")
        )

        Spacer(modifier = Modifier.height(12.dp))

        NavigationDrawerItem(
            label = { Text("Sync Settings Center", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            selected = selectedIndex == 5,
            onClick = { onSelect(5) },
            icon = { Icon(Icons.Default.Sync, contentDescription = null) },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = CosmicPrimary,
                selectedIconColor = CustomFlameOrange,
                selectedTextColor = Color.White,
                unselectedContainerColor = Color.Transparent,
                unselectedIconColor = TextGray,
                unselectedTextColor = TextGray
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("drawer_item_sync_settings")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Section 3: COGNITIVE SERVICES
        Text(
            text = "COGNITIVE ASSISTANT",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = ForestEcoGreen,
            modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 6.dp)
        )

        NavigationDrawerItem(
            label = { Text("Vishwa AI Assistant", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            selected = selectedIndex == 3,
            onClick = { onSelect(3) },
            icon = { Icon(Icons.Default.SmartToy, contentDescription = null) },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = CosmicPrimary,
                selectedIconColor = CustomFlameOrange,
                selectedTextColor = Color.White,
                unselectedContainerColor = Color.Transparent,
                unselectedIconColor = TextGray,
                unselectedTextColor = TextGray
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("drawer_item_ai_assistant")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Section 4: ABOUT SYSTEM
        Text(
            text = "ABOUT PLATFORM",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = AquaticWaveBlue,
            modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 6.dp)
        )

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .testTag("drawer_about_section_card")
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Vishwa Storage",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CustomFlameOrange.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "v1.4.2-beta",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CustomFlameOrange
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

                // Developer Contacts
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Support Contact",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGray
                    )
                    Text(
                        text = "support@vishwa.org",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

                // Security disclaimer
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Security Disclaimer (Testers)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CustomFlameOrange
                    )
                    Text(
                        text = "This application is currently in sandbox testing stage. Your scanned filesystem database logs are processed entirely on-device. No credential tokens or personal storage files are transferred without encryption or prior explicit configuration. Do not upload production sensitive documents or API keys.",
                        fontSize = 9.sp,
                        color = TextGray,
                        lineHeight = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun GlobalProfileAvatarButton(viewModel: FileManagerViewModel) {
    val activeAccount by viewModel.selectedCloudAccount.collectAsStateWithLifecycle()
    
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(
                if (activeAccount != null) CustomFlameOrange else Color.White.copy(alpha = 0.12f)
            )
            .clickable { viewModel.showGlobalAccountSwitcher.value = true }
            .testTag("global_profile_avatar_btn"),
        contentAlignment = Alignment.Center
    ) {
        if (activeAccount != null) {
            val firstLetter = activeAccount?.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
            Text(
                text = firstLetter,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile Switcher",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun ApiKeyPromptDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var keyText by remember { mutableStateOf("") }
    var hideKey by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSurfaceDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("api_key_prompt_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Circular icon header
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CustomFlameOrange.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = CustomFlameOrange,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Gemini API Key Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "A valid Gemini API Key is required to compute AI similarity structure scores.",
                    fontSize = 11.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = keyText,
                    onValueChange = { keyText = it },
                    label = { Text("Enter Gemini API Key") },
                    singleLine = true,
                    visualTransformation = if (hideKey) PasswordVisualTransformation() else VisualTransformation.None,
                    trailingIcon = {
                        IconButton(onClick = { hideKey = !hideKey }) {
                            Icon(
                                imageVector = if (hideKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle key text hiding"
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CustomFlameOrange,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = CustomFlameOrange,
                        unfocusedLabelColor = Color.Gray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_prompt_input_field")
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, Color.Gray),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Color.White)
                    }

                    Button(
                        onClick = {
                            if (keyText.isNotBlank()) {
                                onConfirm(keyText)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CustomFlameOrange),
                        shape = RoundedCornerShape(12.dp),
                        enabled = keyText.isNotBlank(),
                        modifier = Modifier.weight(1f).testTag("btn_api_key_prompt_submit")
                    ) {
                        Text("Start Scan", color = Color.White)
                    }
                }
            }
        }
    }
}
