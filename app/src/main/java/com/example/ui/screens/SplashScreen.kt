package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    // Animation States
    var startAnimate by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }

    val scale by animateFloatAsState(
        targetValue = if (startAnimate) 1f else 0.82f,
        animationSpec = tween(
            durationMillis = 1500,
            easing = EaseOutBack
        ),
        label = "scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (startAnimate) 1f else 0f,
        animationSpec = tween(
            durationMillis = 1200,
            easing = LinearEasing
        ),
        label = "alpha"
    )

    // Trigger animations and progress
    LaunchedEffect(Unit) {
        startAnimate = true
        // Animate progress bar simulating security disk-integrity checking and cloud alignment index
        val steps = 100
        for (i in 1..steps) {
            delay(18)
            progress = i / 100f
        }
        delay(300)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalDarkBg)
            .testTag("app_splash_screen")
    ) {
        // Immersive Portrait Background Artwork
        Image(
            painter = painterResource(id = R.drawable.img_splash_artwork_new_1781816989918),
            contentDescription = "Cosmic Splash Screen Illustration",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Backdrop dark gradient overlay to ensure perfect contrast and text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.3f),
                            CharcoalDarkBg.copy(alpha = 0.95f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // Contents Wrapper
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section (Branding/Slogan)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp)
                    .alpha(alpha),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "V I S H V A",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 6.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(2.dp)
                        .background(AquaticWaveBlue, shape = RoundedCornerShape(1.dp))
                )
            }

            // Central Pulsing Emblem Screen
            Column(
                modifier = Modifier
                    .scale(scale)
                    .alpha(alpha),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Central Outer Multi-color Ring
                Box(
                    modifier = Modifier
                        .size(118.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(
                                    CustomFlameOrange,
                                    AquaticWaveBlue,
                                    ForestEcoGreen,
                                    CustomFlameOrange
                                )
                            )
                        )
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Inner Circular Backdrop
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(DeepSurfaceDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_smart_file_logo_1781816966764),
                            contentDescription = "VISHVA Logo Icon",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "VVF File Manager Ultra",
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "VISHVA VIJAYAA COGNITIVE PLATFORM",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextGray,
                    letterSpacing = 1.5.sp
                )
            }

            // Bottom loading progress & copyright information
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(alpha)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Loading bar mimicking data cataloging
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .width(180.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = CustomFlameOrange,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Indexing filesystem blocks...",
                    color = TextGray.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "© 2026 VISHVA VIJAYAA FOUNDATION. All Rights Reserved.",
                    color = TextGray.copy(alpha = 0.5f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
