package com.example.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.FileManagerViewModel
import com.example.ui.theme.*
import com.example.GlobalProfileAvatarButton

@Composable
fun StorageAnalyticsScreen(
    viewModel: FileManagerViewModel,
    onMenuClick: () -> Unit
) {
    val normalFiles by viewModel.normalFiles.collectAsStateWithLifecycle()
    val junkFiles by viewModel.junkFiles.collectAsStateWithLifecycle()

    // Calculate exact data sets
    val imagesList = normalFiles.filter { it.category == "Images" }
    val videosList = normalFiles.filter { it.category == "Videos" }
    val documentsList = normalFiles.filter { it.category != "Images" && it.category != "Videos" }

    val imagesSize = imagesList.sumOf { it.size }
    val videosSize = videosList.sumOf { it.size }
    val documentsSize = documentsList.sumOf { it.size }
    val junkSize = junkFiles.sumOf { it.size }

    val imagesCount = imagesList.size
    val videosCount = videosList.size
    val documentsCount = documentsList.size
    val junkCount = junkFiles.size

    val totalBytes = imagesSize + videosSize + documentsSize + junkSize

    val formattedImagesSize = viewModel.formatFileSize(imagesSize)
    val formattedVideosSize = viewModel.formatFileSize(videosSize)
    val formattedDocumentsSize = viewModel.formatFileSize(documentsSize)
    val formattedJunkSize = viewModel.formatFileSize(junkSize)
    val formattedTotalSize = viewModel.formatFileSize(totalBytes)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalDarkBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            AnalyticsHeaderBanner(viewModel = viewModel, onMenuClick = onMenuClick)

            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section Title
                Text(
                    text = "Storage Disk Architecture",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // D3 Storage Pie Chart Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .testTag("storage_d3_pie_chart_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepSurfaceDark)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Interactive Donut Visualization (D3.js)",
                            color = TextGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // D3.js WebView component
                        D3PieChart(
                            imagesSize = imagesSize,
                            imagesFormatted = formattedImagesSize,
                            videosSize = videosSize,
                            videosFormatted = formattedVideosSize,
                            docsSize = documentsSize,
                            docsFormatted = formattedDocumentsSize,
                            junkSize = junkSize,
                            junkFormatted = formattedJunkSize,
                            totalBytes = totalBytes,
                            totalBytesFormatted = formattedTotalSize,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }

                // Grid of partition list breakdowns
                Text(
                    text = "File Categories Weight Breakdown",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Images Details Card
                CategoryWeightRow(
                    title = "Images & Photos",
                    sizeText = formattedImagesSize,
                    countText = "$imagesCount files",
                    color = CustomFlameOrange,
                    icon = Icons.Default.Image,
                    testTag = "analytics_category_images"
                )

                // Videos Details Card
                CategoryWeightRow(
                    title = "Videos & Media",
                    sizeText = formattedVideosSize,
                    countText = "$videosCount files",
                    color = AquaticWaveBlue,
                    icon = Icons.Default.Videocam,
                    testTag = "analytics_category_videos"
                )

                // Documents & Audio Details Card
                CategoryWeightRow(
                    title = "Documents & Audio",
                    sizeText = formattedDocumentsSize,
                    countText = "$documentsCount files",
                    color = ForestEcoGreen,
                    icon = Icons.Default.Description,
                    testTag = "analytics_category_docs"
                )

                // Junk Details Card
                CategoryWeightRow(
                    title = "Junk, Cache & Temporary",
                    sizeText = formattedJunkSize,
                    countText = "$junkCount files",
                    color = Color(0xFFA259FF),
                    icon = Icons.Default.DeleteSweep,
                    testTag = "analytics_category_junk"
                )
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun AnalyticsHeaderBanner(
    viewModel: FileManagerViewModel,
    onMenuClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        CosmicPrimary,
                        CharcoalDarkBg
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Responsive menu button
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.testTag("analytics_header_hamburger_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open Navigation Menu",
                        tint = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.width(10.dp))
                
                Column {
                    Text(
                        text = "VISHVA ANALYTICS",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Advanced Disk Topology Engine",
                        fontSize = 9.sp,
                        color = AquaticWaveBlue,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // User switcher
            GlobalProfileAvatarButton(viewModel = viewModel)
        }
    }
}

@Composable
fun CategoryWeightRow(
    title: String,
    sizeText: String,
    countText: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DeepSurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Side indicator color bar with round icon inside
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(color.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = countText,
                        color = TextGray,
                        fontSize = 11.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = sizeText,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
                // Small indicator cap
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(3.dp)
                        .background(color, shape = RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun D3PieChart(
    imagesSize: Long,
    imagesFormatted: String,
    videosSize: Long,
    videosFormatted: String,
    docsSize: Long,
    docsFormatted: String,
    junkSize: Long,
    junkFormatted: String,
    totalBytes: Long,
    totalBytesFormatted: String,
    modifier: Modifier = Modifier
) {
    // Dynamically compile an SVG donut visualization utilizing the D3.js engine
    val chartHtml = remember(imagesSize, videosSize, docsSize, junkSize, totalBytes) {
        val imagesNorm = if (imagesSize < 0) 0L else imagesSize
        val videosNorm = if (videosSize < 0) 0L else videosSize
        val docsNorm = if (docsSize < 0) 0L else docsSize
        val junkNorm = if (junkSize < 0) 0L else junkSize
        val totalNorm = if (totalBytes <= 0) 1L else totalBytes

        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
            <style>
                body, html {
                    margin: 0;
                    padding: 0;
                    width: 100%;
                    height: 100%;
                    overflow: hidden;
                    background-color: transparent !important;
                    font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                    color: #FFFFFF;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                }
                #chart-container {
                    width: 100%;
                    height: 100%;
                    position: relative;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                }
                #tooltip {
                    position: absolute;
                    background: rgba(17, 29, 60, 0.95);
                    border: 1px solid rgba(254, 111, 39, 0.4);
                    border-radius: 8px;
                    padding: 8px 12px;
                    font-size: 11px;
                    color: #ffffff;
                    pointer-events: none;
                    opacity: 0;
                    transition: opacity 0.25s ease;
                    box-shadow: 0 4px 12px rgba(0,0,0,0.4);
                    z-index: 100;
                    white-space: nowrap;
                    text-align: center;
                }
                svg {
                    display: block;
                    max-width: 100%;
                    max-height: 100%;
                }
                .arc {
                    transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
                    cursor: pointer;
                }
                .arc:hover {
                    opacity: 0.85;
                    filter: drop-shadow(0px 4px 8px rgba(0,0,0,0.5));
                }
            </style>
            <!-- Load standard script with local/remote loading -->
            <script src="https://cdnjs.cloudflare.com/ajax/libs/d3/7.8.5/d3.min.js"></script>
        </head>
        <body>
            <div id="chart-container">
                <div id="chart"></div>
                <div id="tooltip"></div>
            </div>

            <script>
                var data = [
                    { label: "Images", value: $imagesNorm, formatted: "$imagesFormatted", color: "#FE6F27" },
                    { label: "Videos", value: $videosNorm, formatted: "$videosFormatted", color: "#2CB6FF" },
                    { label: "Documents", value: $docsNorm, formatted: "$docsFormatted", color: "#2BB168" },
                    { label: "Junk Files", value: $junkNorm, formatted: "$junkFormatted", color: "#A259FF" }
                ].filter(function(d) { return d.value > 0; });

                if (data.length === 0) {
                    data = [
                        { label: "Empty Space", value: 1, formatted: "0 B", color: "#222C44" }
                    ];
                }

                var width = 280;
                var height = 280;
                var radius = Math.min(width, height) / 2;

                try {
                    if (typeof d3 !== "undefined") {
                        renderD3();
                    } else {
                        throw new Error();
                    }
                } catch (err) {
                    renderFallback();
                }

                function renderD3() {
                    var svg = d3.select("#chart")
                        .append("svg")
                        .attr("width", width)
                        .attr("height", height)
                        .append("g")
                        .attr("transform", "translate(" + width / 2 + "," + height / 2 + ")");

                    // Add slight padding to sectors
                    var pie = d3.pie()
                        .sort(null)
                        .value(function(d) { return d.value; })
                        .padAngle(0.02);

                    var arc = d3.arc()
                        .innerRadius(radius * 0.55)
                        .outerRadius(radius * 0.88)
                        .cornerRadius(4);

                    var arcHover = d3.arc()
                        .innerRadius(radius * 0.55)
                        .outerRadius(radius * 0.95)
                        .cornerRadius(4);

                    var path = svg.selectAll(".arc")
                        .data(pie(data))
                        .enter()
                        .append("path")
                        .attr("class", "arc")
                        .attr("d", arc)
                        .attr("fill", function(d) { return d.data.color; })
                        .style("stroke", "#111726")
                        .style("stroke-width", "2px")
                        .on("mouseover", function(event, d) {
                            d3.select(this)
                                .transition()
                                .duration(150)
                                .attr("d", arcHover);

                            var tooltip = d3.select("#tooltip");
                            tooltip.style("opacity", 1)
                                .html("<strong>" + d.data.label + "</strong><br/>" + d.data.formatted);
                        })
                        .on("mousemove", function(event) {
                            var tooltip = d3.select("#tooltip");
                            var containerRect = document.getElementById("chart-container").getBoundingClientRect();
                            var x = event.clientX - containerRect.left;
                            var y = event.clientY - containerRect.top;
                            tooltip.style("left", (x + 10) + "px")
                                   .style("top", (y - 25) + "px");
                        })
                        .on("mouseout", function() {
                            d3.select(this)
                                .transition()
                                .duration(150)
                                .attr("d", arc);

                            d3.select("#tooltip").style("opacity", 0);
                        });

                    // Animated entry
                    path.transition()
                        .duration(800)
                        .attrTween("d", function(d) {
                            var interpolate = d3.interpolate({ startAngle: 0, endAngle: 0 }, d);
                            return function(t) {
                                return arc(interpolate(t));
                            };
                        });

                    // Center Labels
                    var centroid = svg.append("g")
                        .attr("text-anchor", "middle");

                    centroid.append("text")
                        .attr("dy", "-0.3em")
                        .style("fill", "#B0B9C6")
                        .style("font-size", "10px")
                        .style("font-weight", "bold")
                        .style("letter-spacing", "0.5px")
                        .text("USED CORES");

                    centroid.append("text")
                        .attr("dy", "1.0em")
                        .style("fill", "#FFFFFF")
                        .style("font-size", "17px")
                        .style("font-weight", "900")
                        .text("$totalBytesFormatted");
                }

                function renderFallback() {
                    // Simple inline SVG fallback for offline scenarios
                    var chartDiv = document.getElementById("chart");
                    var svgHtml = '<svg width="' + width + '" height="' + height + '"><g transform="translate(' + (width/2) + ',' + (height/2) + ')">';
                    
                    var total = data.reduce(function(acc, item) { return acc + item.value; }, 0);
                    var accumulatedAngle = 0;

                    for (var i = 0; i &lt; data.length; i++) {
                        var d = data[i];
                        var angle = (d.value / total) * 360;
                        if (angle >= 360) {
                            svgHtml += '<circle r="' + (radius * 0.7) + '" fill="none" stroke="' + d.color + '" stroke-width="' + (radius * 0.3) + '" />';
                            break;
                        }

                        var x1 = radius * 0.7 * Math.cos((accumulatedAngle - 90) * Math.PI / 180);
                        var y1 = radius * 0.7 * Math.sin((accumulatedAngle - 90) * Math.PI / 180);
                        var x2 = radius * 0.7 * Math.cos((accumulatedAngle + angle - 90) * Math.PI / 180);
                        var y2 = radius * 0.7 * Math.sin((accumulatedAngle + angle - 90) * Math.PI / 180);

                        var flag = angle > 180 ? 1 : 0;
                        svgHtml += '<path d="M 0 0 L ' + x1 + ' ' + y1 + ' A ' + (radius * 0.7) + ' ' + (radius * 0.7) + ' 0 ' + flag + ' 1 ' + x2 + ' ' + y2 + ' Z" fill="' + d.color + '" stroke="#111726" stroke-width="2" />';
                        accumulatedAngle += angle;
                    }

                    svgHtml += '<circle r="' + (radius * 0.45) + '" fill="#111726" />';
                    svgHtml += '<text text-anchor="middle" dy="0.3em" fill="#FFFFFF" font-size="14" font-weight="900">$totalBytesFormatted</text>';
                    svgHtml += '</g></svg>';
                    chartDiv.innerHTML = svgHtml;
                }
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                webViewClient = WebViewClient()
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }
                // Set background completely transparent
                setBackgroundColor(0)
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://cdnjs.cloudflare.com/api/", chartHtml, "text/html", "UTF-8", null)
        },
        modifier = modifier
    )
}
