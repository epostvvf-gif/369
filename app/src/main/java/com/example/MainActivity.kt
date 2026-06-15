package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AIScreen
import com.example.ui.screens.DriveScreen
import com.example.ui.screens.MainScreen
import com.example.ui.screens.AIChatDrawer
import com.example.ui.theme.CustomFlameOrange
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.FileManagerViewModel
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
    var selectedIndex by remember { mutableStateOf(0) }
    val isChatDrawerOpen by viewModel.isChatDrawerOpen.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            floatingActionButton = {
                if (selectedIndex != 2) { // Hide when already in full-tab AIScreen
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
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("app_bottom_nav_bar")
                ) {
                    // Tab 1: Local
                    NavigationBarItem(
                        selected = selectedIndex == 0,
                        onClick = { selectedIndex = 0 },
                        icon = { Icon(Icons.Default.Storage, contentDescription = "Local") },
                        label = { Text("Local") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CustomFlameOrange,
                            selectedTextColor = CustomFlameOrange,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_tab_local")
                    )

                    // Tab 2: Cloud Drive Sim
                    NavigationBarItem(
                        selected = selectedIndex == 1,
                        onClick = { selectedIndex = 1 },
                        icon = { Icon(Icons.Default.CloudQueue, contentDescription = "Cloud Sim") },
                        label = { Text("Cloud Sim") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CustomFlameOrange,
                            selectedTextColor = CustomFlameOrange,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_tab_cloud")
                    )

                    // Tab 3: Gemini AI Assistant
                    NavigationBarItem(
                        selected = selectedIndex == 2,
                        onClick = { selectedIndex = 2 },
                        icon = { Icon(Icons.Default.SmartToy, contentDescription = "Vishwa AI") },
                        label = { Text("Vishwa AI") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CustomFlameOrange,
                            selectedTextColor = CustomFlameOrange,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_tab_ai")
                    )
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
                    0 -> MainScreen(viewModel = viewModel)
                    1 -> DriveScreen(viewModel = viewModel)
                    2 -> AIScreen(viewModel = viewModel)
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
}
