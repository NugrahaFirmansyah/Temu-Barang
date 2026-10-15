package com.example.ui.screens.report

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.ItemReport
import com.example.ui.components.PRESET_ITEM_PHOTOS
import com.example.ui.components.appText
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportItemScreen(
    initialType: String = "LOST",
    currentUserId: String,
    currentUserName: String,
    currentUserEmail: String,
    onSubmit: (ItemReport) -> Unit,
    onCancel: () -> Unit
) {
    var reportType by remember { mutableStateOf(initialType) } // LOST or FOUND
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Elektronik") }
    var description by remember { mutableStateOf("") }
    var locationName by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf(-6.2088) } // Default Jakarta coordinate
    var longitude by remember { mutableStateOf(106.8456) }
    var dateTime by remember {
        mutableStateOf(SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date()))
    }
    var imageUrl by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }
    var showPhotoPresetDialog by remember { mutableStateOf(false) }
    var showLocationPresetDialog by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    val categories = listOf(
        "Elektronik",
        "Dompet & Dokumen",
        "Kunci Kendaraan",
        "Tas & Ransel",
        "Perhiasan & Jam",
        "Aksesoris",
        "Lainnya"
    )

    val popularLocations = listOf(
        Triple("Stasiun MRT Bundaran HI", -6.1950, 106.8230),
        Triple("Mall Grand Indonesia", -6.1960, 106.8220),
        Triple("Halte TransJakarta Harmoni", -6.1667, 106.8197),
        Triple("Stasiun Gambir", -6.1767, 106.8306),
        Triple("Bandara Soekarno-Hatta Terminal 3", -6.1256, 106.6559),
        Triple("Area Kampus Universitas Indonesia", -6.3644, 106.8286),
        Triple("Gelora Bung Karno (GBK) Senayan", -6.2183, 106.8022),
        Triple("Kawasan Monas Jakarta Pusat", -6.1754, 106.8272)
    )

    val isLost = reportType == "LOST"
    val accentColor = if (isLost) LostRed else FoundGreen

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Header
        Text(
            text = if (isLost) appText("title_report_lost") else appText("title_report_found"),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        // Type Switcher Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isLost) LostRed else Color.Transparent)
                    .clickable { reportType = "LOST" }
                    .padding(vertical = 10.dp)
                    .testTag("report_tab_lost"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = appText("btn_report_lost"),
                    color = if (isLost) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (!isLost) FoundGreen else Color.Transparent)
                    .clickable { reportType = "FOUND" }
                    .padding(vertical = 10.dp)
                    .testTag("report_tab_found"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = appText("btn_report_found"),
                    color = if (!isLost) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // Photo Upload Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = appText("label_photo"),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        FilledTonalButton(
                            onClick = { showPhotoPresetDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_choose_photo_preset")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Collections,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(appText("btn_choose_preset"))
                        }

                        if (imageUrl.isNotBlank()) {
                            TextButton(onClick = { imageUrl = "" }) {
                                Text("Hapus Foto", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        // Title Input
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(appText("label_item_title")) },
            placeholder = { Text("Contoh: iPhone 13 Pro Biru Sierra") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("report_input_title"),
            shape = RoundedCornerShape(12.dp)
        )

        // Category Selector
        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = category,
                onValueChange = {},
                readOnly = true,
                label = { Text(appText("label_category")) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .testTag("report_category_dropdown")
            )

            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }
            ) {
                categories.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat) },
                        onClick = {
                            category = cat
                            categoryExpanded = false
                        }
                    )
                }
            }
        }

        // Description Input
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(appText("label_desc")) },
            placeholder = { Text(appText("hint_desc")) },
            minLines = 3,
            maxLines = 5,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("report_input_desc"),
            shape = RoundedCornerShape(12.dp)
        )

        // Location Card & Preset Selector
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = appText("label_location"),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    TextButton(onClick = { showLocationPresetDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pilih Titik Populer")
                    }
                }

                OutlinedTextField(
                    value = locationName,
                    onValueChange = { locationName = it },
                    placeholder = { Text("Contoh: Halte TransJakarta Bundaran HI") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_input_location"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Koordinat: Lat ${String.format("%.4f", latitude)}, Lng ${String.format("%.4f", longitude)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Date and Time Input
        OutlinedTextField(
            value = dateTime,
            onValueChange = { dateTime = it },
            label = { Text(appText("label_date_time")) },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("report_input_datetime"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Submit Button
        Button(
            onClick = {
                if (title.isBlank()) return@Button
                isSubmitting = true
                val newReport = ItemReport(
                    userId = currentUserId,
                    userName = currentUserName,
                    userEmail = currentUserEmail,
                    type = reportType,
                    title = title.trim(),
                    category = category,
                    description = description.trim(),
                    locationName = locationName.ifBlank { "Lokasi belum ditentukan" }.trim(),
                    latitude = latitude,
                    longitude = longitude,
                    dateTimeLostOrFound = dateTime,
                    imageUrl = imageUrl,
                    status = "OPEN",
                    isVerified = false
                )
                onSubmit(newReport)
            },
            enabled = title.isNotBlank() && !isSubmitting,
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("btn_submit_report")
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = appText("btn_submit_report"),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }

    // Preset Photo Selection Dialog
    if (showPhotoPresetDialog) {
        Dialog(onDismissRequest = { showPhotoPresetDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Pilih Contoh Foto Barang",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.height(300.dp)
                    ) {
                        items(PRESET_ITEM_PHOTOS) { (name, url) ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .clickable {
                                        imageUrl = url
                                        showPhotoPresetDialog = false
                                    }
                            ) {
                                Column {
                                    AsyncImage(
                                        model = url,
                                        contentDescription = name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(90.dp)
                                    )
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(6.dp),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = { showPhotoPresetDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Tutup")
                    }
                }
            }
        }
    }

    // Preset Location Selection Dialog
    if (showLocationPresetDialog) {
        Dialog(onDismissRequest = { showLocationPresetDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Pilih Lokasi Populer",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .heightIn(max = 350.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        popularLocations.forEach { (name, lat, lon) ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        locationName = name
                                        latitude = lat
                                        longitude = lon
                                        showLocationPresetDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = { showLocationPresetDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Tutup")
                    }
                }
            }
        }
    }
}
