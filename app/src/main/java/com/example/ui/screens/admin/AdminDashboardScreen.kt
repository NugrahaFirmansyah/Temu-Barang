package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ItemReport
import com.example.ui.components.ItemReportCard
import com.example.ui.components.appText
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    items: List<ItemReport>,
    onVerifyItem: (String, Boolean) -> Unit,
    onFlagSpam: (String) -> Unit,
    onDeleteItem: (String) -> Unit,
    onBack: () -> Unit,
    onItemClick: (ItemReport) -> Unit
) {
    var adminTab by remember { mutableStateOf("PENDING") } // PENDING, ALL, SPAM

    val totalCount = items.size
    val verifiedCount = items.count { it.isVerified }
    val spamCount = items.count { it.status == "FLAGGED_SPAM" }

    val filteredItems = remember(items, adminTab) {
        when (adminTab) {
            "PENDING" -> items.filter { !it.isVerified && it.status != "FLAGGED_SPAM" }
            "SPAM" -> items.filter { it.status == "FLAGGED_SPAM" }
            else -> items
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = LostRed
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = appText("admin_dashboard"),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .testTag("admin_dashboard_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminStatCard(
                        count = totalCount,
                        label = appText("admin_stat_total"),
                        color = PrimaryLight,
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatCard(
                        count = verifiedCount,
                        label = appText("admin_stat_verified"),
                        color = FoundGreen,
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatCard(
                        count = spamCount,
                        label = appText("admin_stat_spam"),
                        color = LostRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Admin Filter Tabs
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                ) {
                    AdminTabItem(
                        title = "Menunggu (${items.count { !it.isVerified && it.status != "FLAGGED_SPAM" }})",
                        isSelected = adminTab == "PENDING",
                        onClick = { adminTab = "PENDING" },
                        modifier = Modifier.weight(1f)
                    )
                    AdminTabItem(
                        title = "Semua ($totalCount)",
                        isSelected = adminTab == "ALL",
                        onClick = { adminTab = "ALL" },
                        modifier = Modifier.weight(1f)
                    )
                    AdminTabItem(
                        title = "Spam ($spamCount)",
                        isSelected = adminTab == "SPAM",
                        onClick = { adminTab = "SPAM" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Reports with Moderation Action Buttons
            if (filteredItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tidak ada laporan dalam kategori ini.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredItems, key = { it.id }) { item ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            ItemReportCard(
                                item = item,
                                onClick = { onItemClick(item) }
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Tindakan Verifikasi & Moderasi:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Toggle Verification
                                if (!item.isVerified) {
                                    Button(
                                        onClick = { onVerifyItem(item.id, true) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = VerifiedBlue)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(appText("btn_verify_item"))
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { onVerifyItem(item.id, false) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Batal Verifikasi")
                                    }
                                }

                                // Flag as spam / restore
                                if (item.status != "FLAGGED_SPAM") {
                                    Button(
                                        onClick = { onFlagSpam(item.id) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = LostRed)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(appText("btn_flag_spam"))
                                    }
                                } else {
                                    Button(
                                        onClick = { onVerifyItem(item.id, false) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = FoundGreen)
                                    ) {
                                        Text("Pulihkan")
                                    }
                                }

                                // Delete
                                IconButton(
                                    onClick = { onDeleteItem(item.id) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Hapus",
                                        tint = MaterialTheme.colorScheme.error
                                    )
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
private fun AdminStatCard(count: Int, label: String, color: Color, modifier: Modifier) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun AdminTabItem(title: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
