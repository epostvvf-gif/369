package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.viewmodel.FileManagerViewModel
import com.example.viewmodel.PreviewFile
import com.example.ui.theme.*
import java.io.File
import kotlin.math.sin

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FilePreviewDialog(
    previewFile: PreviewFile,
    viewModel: FileManagerViewModel,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable(enabled = true, onClick = { onDismiss() })
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .widthIn(max = 600.dp)
                    .clickable(enabled = false, onClick = {}) // prevent click-through
                    .testTag("file_preview_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CharcoalDarkBg),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    // Header Section
                    PreviewHeader(
                        file = previewFile,
                        viewModel = viewModel,
                        onClose = onDismiss
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.10f))

                    // Content Section with dynamic category renderers
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(CharcoalDarkBg),
                        contentAlignment = Alignment.Center
                    ) {
                        when (previewFile.category) {
                            "Images" -> ImagePreviewer(file = previewFile)
                            "Documents" -> DocumentPreviewer(file = previewFile)
                            "Audio" -> AudioPreviewer(file = previewFile)
                            "Videos" -> VideoPreviewer(file = previewFile)
                            else -> OthersPreviewer(file = previewFile)
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.10f))

                    // Footer Actions Bar
                    PreviewFooter(
                        file = previewFile,
                        viewModel = viewModel,
                        onClose = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
fun PreviewHeader(
    file: PreviewFile,
    viewModel: FileManagerViewModel,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                val icon = when (file.category) {
                    "Images" -> Icons.Default.Image
                    "Documents" -> Icons.Default.Description
                    "Audio" -> Icons.Default.MusicNote
                    "Videos" -> Icons.Default.Videocam
                    else -> Icons.Default.InsertDriveFile
                }
                val iconColor = when (file.category) {
                    "Images" -> CustomFlameOrange
                    "Documents" -> AquaticWaveBlue
                    "Audio" -> ForestEcoGreen
                    "Videos" -> MaterialTheme.colorScheme.primary
                    else -> Color.LightGray
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = file.name,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = viewModel.formatFileSize(file.size),
                        fontSize = 11.sp,
                        color = TextGray
                    )
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.3f))
                    )
                    val originLabel = if (file.isCloud) "Google Drive" else "Local Sandbox"
                    val originColor = if (file.isCloud) AquaticWaveBlue else ForestEcoGreen
                    Text(
                        text = originLabel,
                        fontSize = 11.sp,
                        color = originColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        IconButton(
            onClick = { onClose() },
            modifier = Modifier
                .size(40.dp)
                .background(Color.White.copy(alpha = 0.06f), CircleShape)
                .testTag("btn_close_preview")
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close preview",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun PreviewFooter(
    file: PreviewFile,
    viewModel: FileManagerViewModel,
    onClose: () -> Unit
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
                text = "SANDBOX STATUS",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 1.sp
            )
            val desc = if (file.isSafe) "Encrypted Vault Storage" else "Integrated System Path"
            Text(
                text = desc,
                fontSize = 11.sp,
                color = if (file.isSafe) ForestEcoGreen else AquaticWaveBlue,
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (file.isCloud) {
                Button(
                    onClick = {
                        // Mock restore/download to local
                        onClose()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AquaticWaveBlue),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Download", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            } else {
                Button(
                    onClick = {
                        // Mock share action
                        onClose()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White,
                        containerColor = Color.White.copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.LightGray)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ImagePreviewer(file: PreviewFile) {
    var hasError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!file.isCloud && file.path != null && !hasError) {
            val localFile = File(file.path)
            AsyncImage(
                model = localFile,
                contentDescription = file.name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .testTag("image_preview_coil"),
                contentScale = ContentScale.Fit,
                onError = { hasError = true }
            )
        } else {
            hasError = true
        }

        if (hasError) {
            // Render a state-of-the-art cinematic image placeholder
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .background(DeepSurfaceDark, shape = RoundedCornerShape(18.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.04f), shape = RoundedCornerShape(18.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CustomFlameOrange.copy(alpha = 0.08f))
                        .drawBehind {
                            // Draw an abstract high-tech lens circle
                            drawCircle(
                                color = CustomFlameOrange.copy(alpha = 0.35f),
                                radius = size.minDimension / 3f,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.InsertPhoto,
                        contentDescription = null,
                        tint = CustomFlameOrange,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = file.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Simulated camera parameters to enrich visual craft
                Text(
                    text = "JPEG • sRGB • 4032 x 3024 • ISO 100 • f/1.8 • 1/120s",
                    fontSize = 11.sp,
                    color = TextGray
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CustomFlameOrange.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "SANDBOX INTEGRATION ONLY",
                        fontSize = 9.sp,
                        color = CustomFlameOrange,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
fun DocumentPreviewer(file: PreviewFile) {
    val isTxt = file.name.substringAfterLast('.', "").lowercase() in listOf("txt", "log", "cache")
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isTxt) {
            // Text Document view reader
            val lines = file.textContent?.split("\n") ?: emptyList()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DeepSurfaceDark, shape = RoundedCornerShape(18.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(18.dp))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MONOSPACED SECURE DOCUMENT VIEW",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = AquaticWaveBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${lines.size} lines",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextGray
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .fillMaxWidth()
                ) {
                    itemsIndexed(lines) { index, line ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                            Text(
                                text = String.format("[%03d]", index + 1),
                                color = Color.White.copy(alpha = 0.25f),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier.width(36.dp)
                            )
                            Text(
                                text = line,
                                color = TextWhite,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        } else {
            // Complex PDF/Word/Excel mock structure
            val docTypeLabel = when {
                file.name.contains(".pdf", ignoreCase = true) -> "PDF METADATA RECONSTRUCT"
                file.name.contains(".xlsx", ignoreCase = true) || file.name.contains(".xls", ignoreCase = true) -> "SPREADSHEET MATRIX ENGINE"
                else -> "OFFICE SYSTEM METADATA"
            }
            val primaryColor = if (file.name.contains(".xlsx")) ForestEcoGreen else AquaticWaveBlue
            val icon = if (file.name.contains(".xlsx")) Icons.Default.GridOn else Icons.Default.HistoryEdu

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DeepSurfaceDark, shape = RoundedCornerShape(18.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, contentDescription = null, tint = primaryColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = docTypeLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                    }
                    Text(
                        text = "Page 1 of 6",
                        fontSize = 10.sp,
                        color = TextGray
                    )
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "VISHWA TRUSTEE SECURE VERIFIED LEDGER",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Document Reference: VSHA-2026-DX-8219A\nHash Signature: sha256_b48f98...92cc\nRelease Code: VFC-2026",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextGray,
                        lineHeight = 15.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    )

                    Text(
                        text = "STRUCTURAL SECTIONS & EXCEL COLUMNS:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )

                    // Mock data rows representing columns
                    listOf(
                        "01. Tax Exemption Release ID" to "Exempted status verified by vishwa trust",
                        "02. Financial Account Allocation" to "Assigned to primary security vault offline ledger",
                        "03. Trustee Ledger Audit Verification" to "Internal checks passed on standard filesystems",
                        "04. Secure Private Key Seed Root" to "Wrapped inside Private Vault folder container"
                    ).forEach { (title, subtitle) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(subtitle, fontSize = 10.sp, color = TextGray)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(primaryColor.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                            .border(1.dp, primaryColor.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = primaryColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "✓ Digitally Signed & Locked by Vishwa Authority",
                                fontSize = 10.sp,
                                color = primaryColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AudioPreviewer(file: PreviewFile) {
    var isPlaying by remember { mutableStateOf(false) }
    var speedMultiplier by remember { mutableStateOf(1.0f) }
    var currentProgress by remember { mutableStateOf(0.40f) }

    // Dynamic wave animation properties
    val infiniteTransition = rememberInfiniteTransition()
    val waveOffset = if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 2 * Math.PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = java.lang.Float.isNaN(0f).let { tween(2000, easing = LinearEasing) },
                repeatMode = RepeatMode.Restart
            )
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Rotating disk vinyl mockup
        val rotationAngle = if (isPlaying) {
            infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(4000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            remember { mutableStateOf(0f) }
        }

        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(Color.Black)
                .border(6.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                .drawBehind {
                    // Classic record groove lines
                    for (i in 1..4) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.1f * i),
                            radius = (size.minDimension / 10) * i,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                    // Needle line simulator
                    if (isPlaying) {
                        drawLine(
                            color = CustomFlameOrange,
                            start = center,
                            end = Offset(
                                center.x + sin(rotationAngle.value * Math.PI / 180f).toFloat() * 50f,
                                center.y - sin((rotationAngle.value + 45) * Math.PI / 180f).toFloat() * 50f
                            ),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(CustomFlameOrange),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Equalizer Waveform canvas drawing
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .drawBehind {
                    val width = size.width
                    val height = size.height
                    val barCount = 40
                    val barWidth = width / (barCount * 1.5f)
                    val spacing = barWidth * 0.5f

                    for (i in 0 until barCount) {
                        val progressRatio = i.toFloat() / barCount.toFloat()
                        val waveModifier = if (isPlaying) {
                            sin(progressRatio * 10f + waveOffset.value).toFloat()
                        } else {
                            sin(progressRatio * 10f).toFloat()
                        }
                        val barHeight = (height * 0.3f) + (height * 0.5f * kotlin.math.abs(waveModifier))
                        val x = i * (barWidth + spacing) + spacing
                        val paintColor = if (progressRatio <= currentProgress) ForestEcoGreen else Color.White.copy(alpha = 0.2f)
                        
                        drawRect(
                            color = paintColor,
                            topLeft = Offset(x, (height - barHeight) / 2f),
                            size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                        )
                    }
                }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Linear Progress slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val elapsed = "01:24"
            val duration = "03:45"
            Text(elapsed, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextGray)
            Text(duration, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextGray)
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Play controllers layout
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Speed adjustment toggle
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .clickable {
                        speedMultiplier = when (speedMultiplier) {
                            1.0f -> 1.5f
                            1.5f -> 2.0f
                            else -> 1.0f
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("${speedMultiplier}x", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            IconButton(
                onClick = { isPlaying = !isPlaying },
                modifier = Modifier
                    .size(54.dp)
                    .background(ForestEcoGreen, CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(
                onClick = { currentProgress = (currentProgress + 0.1f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Skip Forward",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun VideoPreviewer(file: PreviewFile) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentAspectExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mock video preview display matching visual directions
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (currentAspectExpanded) 1f else 1.77f)
                .background(Color.Black, shape = RoundedCornerShape(18.dp))
                .border(1.dp, Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(18.dp))
                .clickable { isPlaying = !isPlaying },
            contentAlignment = Alignment.Center
        ) {
            // Play overlay button
            if (!isPlaying) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Video",
                        tint = AquaticWaveBlue,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Bottom controls overlay list
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.61f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Playback state indicator",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("0:24 / 4:18", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AquaticWaveBlue.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("1080p", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AquaticWaveBlue)
                        }

                        IconButton(
                            onClick = { currentAspectExpanded = !currentAspectExpanded },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (currentAspectExpanded) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Resize Aspect Ratio",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = file.name,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Video Codec: H.264 (AVC) • Frame Rate: 30 fps",
            fontSize = 10.sp,
            color = TextGray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun OthersPreviewer(file: PreviewFile) {
    // Elegant system hex viewer represent binary data
    val mockAddresses = listOf("00000000", "00000010", "00000020", "00000030", "00000040", "00000050")
    val mockHexPairs = listOf(
        "4F 62 73 6F 6C 65 74 65  5F 6C 6F 67 73 5F 64 61",
        "74 61 20 6F 66 66 6C 69  6E 65 20 66 69 6C 65 73",
        "20 73 61 6E 64 62 6F 78  20 63 6F 6E 74 65 6E 74",
        "20 74 65 6D 70 6F 72 61  72 79 20 63 61 63 68 65",
        "20 6A 75 6E 6B 20 64 65  74 65 63 74 65 64 20 2E",
        "20 73 79 73 74 65 6D 20  65 6E 67 69 6E 65 20 76"
    )
    val mockAscii = listOf(
        "Obsolete_logs_da",
        "ta offline files",
        " sandbox content",
        " temporary cache",
        " junk detected .",
        " system engine v"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepSurfaceDark, shape = RoundedCornerShape(18.dp))
                .border(1.dp, Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(18.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "BINARY CORE INSPECTOR",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.LightGray
                )
                Text(
                    text = "OFFSET [HEXADECIMAL-MATRIX] ASCII",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Magenta
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (i in mockAddresses.indices) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = mockAddresses[i],
                            color = Color.Magenta.copy(alpha = 0.6f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                        Text(
                            text = mockHexPairs[i],
                            color = Color.White.copy(alpha = 0.9f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = mockAscii[i],
                            color = AquaticWaveBlue,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(10.dp))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = CustomFlameOrange, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Notice: RAW binary files lack dedicated parsing protocols.",
                color = TextGray,
                fontSize = 10.sp
            )
        }
    }
}
