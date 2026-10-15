package com.example.ui.screens.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ItemReport
import com.example.ui.components.StatusBadge
import com.example.ui.components.appText
import com.example.ui.components.getCategoryIcon
import com.example.ui.theme.*
import kotlin.math.sqrt

@Composable
fun DigitalMapScreen(
    items: List<ItemReport>,
    currentUserId: String,
    onItemClick: (ItemReport) -> Unit,
    onStartChat: (ItemReport) -> Unit
) {
    var filterType by remember { mutableStateOf("ALL") } // ALL, LOST, FOUND
    var selectedItem by remember { mutableStateOf<ItemReport?>(null) }

    // Map viewport pan & zoom state
    var panOffsetX by remember { mutableStateOf(0f) }
    var panOffsetY by remember { mutableStateOf(0f) }
    var zoomScale by remember { mutableStateOf(1.0f) }

    val filteredItems = remember(items, filterType) {
        items.filter { item ->
            when (filterType) {
                "LOST" -> item.type == "LOST"
                "FOUND" -> item.type == "FOUND"
                else -> true
            }
        }
    }

    // Reference center (Monas / Central Jakarta)
    val refLat = -6.2000
    val refLon = 106.8250

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Interactive Canvas Map
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        panOffsetX += dragAmount.x
                        panOffsetY += dragAmount.y
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f + panOffsetX
            val centerY = height / 2f + panOffsetY

            // Background digital terrain grid
            val gridSize = 60f * zoomScale
            val startX = (centerX % gridSize)
            val startY = (centerY % gridSize)

            var x = startX
            while (x < width) {
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.25f),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1f
                )
                x += gridSize
            }

            var y = startY
            while (y < height) {
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.25f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
                y += gridSize
            }

            // Stylized roads / city arteries
            val roadColor = Color(0xFF0284C7).copy(alpha = 0.15f)
            drawLine(roadColor, Offset(0f, centerY - 80f), Offset(width, centerY + 80f), strokeWidth = 24f * zoomScale)
            drawLine(roadColor, Offset(centerX - 120f, 0f), Offset(centerX + 60f, height), strokeWidth = 20f * zoomScale)
            drawLine(roadColor, Offset(0f, centerY + 140f), Offset(width, centerY - 60f), strokeWidth = 16f * zoomScale)

            // Current User Central Radar Ring
            drawCircle(
                color = PrimaryLight.copy(alpha = 0.15f),
                radius = 120f * zoomScale,
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = PrimaryLight.copy(alpha = 0.3f),
                radius = 60f * zoomScale,
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = PrimaryLight,
                radius = 8f * zoomScale,
                center = Offset(centerX, centerY)
            )

            // Draw Item Pins
            filteredItems.forEach { item ->
                // Project lat/lng offset in pixels
                val deltaX = ((item.longitude - refLon) * 2000.0 * zoomScale).toFloat()
                val deltaY = (-(item.latitude - refLat) * 2000.0 * zoomScale).toFloat()

                val pinX = centerX + deltaX
                val pinY = centerY + deltaY

                val isLost = item.type == "LOST"
                val pinColor = if (isLost) LostRed else FoundGreen

                // Outer Glow ring
                drawCircle(
                    color = pinColor.copy(alpha = 0.25f),
                    radius = 24f,
                    center = Offset(pinX, pinY)
                )

                // Pin Circle
                drawCircle(
                    color = pinColor,
                    radius = 16f,
                    center = Offset(pinX, pinY)
                )

                // White Center Dot
                drawCircle(
                    color = Color.White,
                    radius = 6f,
                    center = Offset(pinX, pinY)
                )

                // Pin Stem
                val stemPath = Path().apply {
                    moveTo(pinX - 6f, pinY + 6f)
                    lineTo(pinX, pinY + 22f)
                    lineTo(pinX + 6f, pinY + 6f)
                    close()
                }
                drawPath(stemPath, color = pinColor)
            }
        }

        // Invisible touch interceptor overlay for pin clicks
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val width = constraints.maxWidth.toFloat()
            val height = constraints.maxHeight.toFloat()
            val centerX = width / 2f + panOffsetX
            val centerY = height / 2f + panOffsetY

            filteredItems.forEach { item ->
                val deltaX = ((item.longitude - refLon) * 2000.0 * zoomScale).toFloat()
                val deltaY = (-(item.latitude - refLat) * 2000.0 * zoomScale).toFloat()

                val pinX = (centerX + deltaX).toInt()
                val pinY = (centerY + deltaY).toInt()

                if (pinX in 0..constraints.maxWidth && pinY in 0..constraints.maxHeight) {
                    Box(
                        modifier = Modifier
                            .offset((pinX - 24).dp, (pinY - 24).dp)
                            .size(48.dp)
                            .clickable { selectedItem = item }
                    )
                }
            }
        }

        // Top Filter Bar
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 6.dp,
            shadowElevation = 4.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MapFilterChip(
                    label = appText("map_filter_all"),
                    isSelected = filterType == "ALL",
                    onClick = { filterType = "ALL" },
                    color = PrimaryLight
                )
                MapFilterChip(
                    label = appText("map_filter_lost"),
                    isSelected = filterType == "LOST",
                    onClick = { filterType = "LOST" },
                    color = LostRed
                )
                MapFilterChip(
                    label = appText("map_filter_found"),
                    isSelected = filterType == "FOUND",
                    onClick = { filterType = "FOUND" },
                    color = FoundGreen
                )
            }
        }

        // Zoom Controls & Recenter
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = { zoomScale = (zoomScale * 1.25f).coerceAtMost(3.0f) },
                modifier = Modifier.size(44.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In")
            }

            FloatingActionButton(
                onClick = { zoomScale = (zoomScale / 1.25f).coerceAtLeast(0.5f) },
                modifier = Modifier.size(44.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
            }

            FloatingActionButton(
                onClick = {
                    panOffsetX = 0f
                    panOffsetY = 0f
                    zoomScale = 1.0f
                },
                modifier = Modifier.size(44.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Recenter")
            }
        }

        // Selected Pin Bottom Card
        AnimatedVisibility(
            visible = selectedItem != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selectedItem?.let { item ->
                Card(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("map_item_preview_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatusBadge(status = item.status, isVerified = item.isVerified)

                            IconButton(
                                onClick = { selectedItem = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (item.imageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = item.imageUrl,
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = getCategoryIcon(item.category),
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = "${item.category} • ${if (item.type == "LOST") "Hilang" else "Ditemukan"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (item.type == "LOST") LostRed else FoundGreen
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = item.locationName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onItemClick(item) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Detail Lengkap")
                            }

                            if (item.userId != currentUserId) {
                                Button(
                                    onClick = { onStartChat(item) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Chat,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(appText("btn_chat_safe"))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    color: Color
) {
    Surface(
        color = if (isSelected) color.copy(alpha = 0.2f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
