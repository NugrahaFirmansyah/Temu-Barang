package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.model.ChatMessage
import com.example.data.model.ChatRoom
import com.example.data.model.ItemMatch
import com.example.data.model.ItemReport
import com.example.data.model.UserNotification
import com.example.data.model.UserProfile
import com.example.data.repository.AiMatchingService
import com.example.data.repository.ChatRepository
import com.example.data.repository.ItemRepository
import com.example.data.repository.NotificationRepository
import com.example.data.repository.UserRepository
import com.example.ui.components.AppTopBar
import com.example.ui.components.appText
import com.example.ui.screens.admin.AdminDashboardScreen
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.auth.performSignOut
import com.example.ui.screens.chat.ChatDetailScreen
import com.example.ui.screens.chat.ChatListScreen
import com.example.ui.screens.detail.ItemDetailDialog
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.map.DigitalMapScreen
import com.example.ui.screens.matching.AiMatchingScreen
import com.example.ui.screens.notifications.NotificationsScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.report.ReportItemScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    val context = LocalContext.current
    var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        MainAppNavigation(user = currentUser!!)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppNavigation(user: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Initialize Repositories with custom database ID
    val db = remember {
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    }

    val itemRepo = remember { ItemRepository(context) }
    val userRepo = remember { UserRepository(db) }
    val notifRepo = remember { NotificationRepository(db) }
    val chatRepo = remember { ChatRepository(db) }
    val aiMatchingService = remember { AiMatchingService(db, notifRepo) }

    // Navigation Screens
    var currentScreen by remember { mutableStateOf("home") } // home, map, report, matching, chats, profile, admin, notifications
    var reportInitialType by remember { mutableStateOf("LOST") }
    var activeChatRoom by remember { mutableStateOf<ChatRoom?>(null) }
    var inspectedItem by remember { mutableStateOf<ItemReport?>(null) }

    // Live States
    val itemsState = remember { mutableStateListOf<ItemReport>() }
    val userProfileState = remember { mutableStateOf<UserProfile?>(null) }
    val notificationsState = remember { mutableStateListOf<UserNotification>() }
    val matchesState = remember { mutableStateListOf<ItemMatch>() }
    val chatRoomsState = remember { mutableStateListOf<ChatRoom>() }
    val currentMessagesState = remember { mutableStateListOf<ChatMessage>() }
    var isRunningMatching by remember { mutableStateOf(false) }

    val isAdmin = user.email == "nugrahafirmansyah49@gmail.com"

    // Sync User Profile on launch
    LaunchedEffect(user.uid) {
        userRepo.saveOrInitProfile(
            userId = user.uid,
            displayName = user.displayName ?: "Pengguna",
            email = user.email ?: "",
            photoUrl = user.photoUrl?.toString()
        )
    }

    // Observe Items in Realtime
    LaunchedEffect(Unit) {
        itemRepo.observeAllItems().collectLatest { fetched ->
            itemsState.clear()
            itemsState.addAll(fetched)
        }
    }

    // Observe User Profile in Realtime
    LaunchedEffect(user.uid) {
        userRepo.observeProfile(user.uid).collectLatest { profile ->
            userProfileState.value = profile
        }
    }

    // Observe Notifications in Realtime
    LaunchedEffect(user.uid) {
        notifRepo.observeUserNotifications(user.uid).collectLatest { fetched ->
            notificationsState.clear()
            notificationsState.addAll(fetched)
        }
    }

    // Observe Chats in Realtime
    LaunchedEffect(user.uid) {
        chatRepo.observeUserChats(user.uid).collectLatest { fetched ->
            chatRoomsState.clear()
            chatRoomsState.addAll(fetched)
        }
    }

    // Observe Messages for Active Chat
    LaunchedEffect(activeChatRoom?.id) {
        activeChatRoom?.id?.let { chatId ->
            chatRepo.observeMessages(chatId).collectLatest { fetched ->
                currentMessagesState.clear()
                currentMessagesState.addAll(fetched)
            }
        }
    }

    val unreadNotifs = notificationsState.count { !it.isRead }

    // Subscreen BackHandler
    BackHandler(enabled = activeChatRoom != null || currentScreen != "home") {
        if (activeChatRoom != null) {
            activeChatRoom = null
        } else if (currentScreen != "home") {
            currentScreen = "home"
        }
    }

    // If viewing a Chat Room
    if (activeChatRoom != null) {
        ChatDetailScreen(
            chatRoom = activeChatRoom!!,
            messages = currentMessagesState,
            currentUserId = user.uid,
            currentUserName = userProfileState.value?.displayName ?: user.displayName ?: "Pengguna",
            onSendMessage = { text ->
                scope.launch {
                    chatRepo.sendMessage(
                        chatId = activeChatRoom!!.id,
                        senderId = user.uid,
                        senderName = userProfileState.value?.displayName ?: user.displayName ?: "Pengguna",
                        text = text
                    )
                }
            },
            onBack = { activeChatRoom = null },
            onMarkResolved = {
                scope.launch {
                    itemRepo.markResolved(activeChatRoom!!.itemId)
                    Toast.makeText(context, "Status barang ditandai telah selesai!", Toast.LENGTH_SHORT).show()
                }
            }
        )
        return
    }

    Scaffold(
        topBar = {
            if (currentScreen != "admin") {
                AppTopBar(
                    title = appText("app_title"),
                    unreadNotificationCount = unreadNotifs,
                    onNotificationClick = { currentScreen = "notifications" },
                    showAdminBadge = isAdmin,
                    onAdminClick = { currentScreen = "admin" }
                )
            }
        },
        bottomBar = {
            if (currentScreen != "admin") {
                NavigationBar(
                    windowInsets = WindowInsets.navigationBars,
                    modifier = Modifier.testTag("main_bottom_nav")
                ) {
                    NavigationBarItem(
                        selected = currentScreen == "home",
                        onClick = { currentScreen = "home" },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text(appText("nav_home")) }
                    )
                    NavigationBarItem(
                        selected = currentScreen == "map",
                        onClick = { currentScreen = "map" },
                        icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
                        label = { Text(appText("nav_map")) }
                    )
                    NavigationBarItem(
                        selected = currentScreen == "report",
                        onClick = {
                            reportInitialType = "LOST"
                            currentScreen = "report"
                        },
                        icon = { Icon(Icons.Default.AddCircle, contentDescription = "Report") },
                        label = { Text(appText("nav_report")) }
                    )
                    NavigationBarItem(
                        selected = currentScreen == "matching",
                        onClick = { currentScreen = "matching" },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Match") },
                        label = { Text(appText("nav_matching")) }
                    )
                    NavigationBarItem(
                        selected = currentScreen == "chats",
                        onClick = { currentScreen = "chats" },
                        icon = { Icon(Icons.Default.Forum, contentDescription = "Chats") },
                        label = { Text(appText("nav_chats")) }
                    )
                    NavigationBarItem(
                        selected = currentScreen == "profile",
                        onClick = { currentScreen = "profile" },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text(appText("nav_profile")) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                "home" -> {
                    HomeScreen(
                        items = itemsState,
                        currentUserId = user.uid,
                        onReportLostClick = {
                            reportInitialType = "LOST"
                            currentScreen = "report"
                        },
                        onReportFoundClick = {
                            reportInitialType = "FOUND"
                            currentScreen = "report"
                        },
                        onItemClick = { item -> inspectedItem = item },
                        onStartMatchingClick = { currentScreen = "matching" }
                    )
                }

                "map" -> {
                    DigitalMapScreen(
                        items = itemsState,
                        currentUserId = user.uid,
                        onItemClick = { item -> inspectedItem = item },
                        onStartChat = { item ->
                            scope.launch {
                                val result = chatRepo.getOrCreateChat(
                                    lostUserId = if (item.type == "LOST") item.userId else user.uid,
                                    foundUserId = if (item.type == "FOUND") item.userId else user.uid,
                                    itemId = item.id,
                                    itemTitle = item.title
                                )
                                result.onSuccess { chatId ->
                                    activeChatRoom = ChatRoom(
                                        id = chatId,
                                        itemId = item.id,
                                        itemTitle = item.title,
                                        participantUids = listOf(user.uid, item.userId)
                                    )
                                }
                            }
                        }
                    )
                }

                "report" -> {
                    ReportItemScreen(
                        initialType = reportInitialType,
                        currentUserId = user.uid,
                        currentUserName = userProfileState.value?.displayName ?: user.displayName ?: "Pengguna",
                        currentUserEmail = user.email ?: "",
                        onSubmit = { newReport ->
                            scope.launch {
                                val res = itemRepo.createReport(newReport)
                                res.onSuccess { createdId ->
                                    val savedItem = newReport.copy(id = createdId)
                                    Toast.makeText(context, appText("msg_report_success"), Toast.LENGTH_LONG).show()
                                    currentScreen = "home"

                                    // Trigger AI Matching in background
                                    scope.launch {
                                        val newMatches = aiMatchingService.findMatchesForItem(savedItem, itemsState)
                                        if (newMatches.isNotEmpty()) {
                                            matchesState.addAll(newMatches)
                                        }
                                    }
                                }
                                res.onFailure {
                                    Toast.makeText(context, "Gagal membuat laporan: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onCancel = { currentScreen = "home" }
                    )
                }

                "matching" -> {
                    AiMatchingScreen(
                        items = itemsState,
                        matches = matchesState,
                        currentUserId = user.uid,
                        isRunningMatching = isRunningMatching,
                        onRunMatching = {
                            scope.launch {
                                isRunningMatching = true
                                val userItems = itemsState.filter { it.userId == user.uid }
                                matchesState.clear()
                                for (uItem in userItems) {
                                    val results = aiMatchingService.findMatchesForItem(uItem, itemsState)
                                    matchesState.addAll(results)
                                }
                                isRunningMatching = false
                                Toast.makeText(context, "Pencocokan AI selesai! Ditemukan ${matchesState.size} kecocokan.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onOpenChat = { otherUid, itemId, itemTitle ->
                            scope.launch {
                                val result = chatRepo.getOrCreateChat(
                                    lostUserId = user.uid,
                                    foundUserId = otherUid,
                                    itemId = itemId,
                                    itemTitle = itemTitle
                                )
                                result.onSuccess { chatId ->
                                    activeChatRoom = ChatRoom(
                                        id = chatId,
                                        itemId = itemId,
                                        itemTitle = itemTitle,
                                        participantUids = listOf(user.uid, otherUid)
                                    )
                                }
                            }
                        },
                        onItemClick = { item -> inspectedItem = item }
                    )
                }

                "chats" -> {
                    ChatListScreen(
                        chatRooms = chatRoomsState,
                        currentUserId = user.uid,
                        onSelectChat = { room -> activeChatRoom = room }
                    )
                }

                "profile" -> {
                    ProfileScreen(
                        userProfile = userProfileState.value,
                        userReports = itemsState.filter { it.userId == user.uid },
                        isAdmin = isAdmin,
                        onOpenAdminDashboard = { currentScreen = "admin" },
                        onUpdateProfile = { name, phone ->
                            scope.launch {
                                userRepo.updateProfile(user.uid, name, phone)
                                Toast.makeText(context, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onSignOut = {
                            val credManager = CredentialManager.create(context)
                            performSignOut(context, credManager, scope) {
                                // Auth state listener resets to AuthScreen
                            }
                        },
                        onItemClick = { item -> inspectedItem = item }
                    )
                }

                "admin" -> {
                    AdminDashboardScreen(
                        items = itemsState,
                        onVerifyItem = { itemId, verified ->
                            scope.launch {
                                itemRepo.setVerified(itemId, verified)
                                Toast.makeText(context, if (verified) "Laporan Diverifikasi" else "Verifikasi Dicabut", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onFlagSpam = { itemId ->
                            scope.launch {
                                itemRepo.flagSpam(itemId)
                                Toast.makeText(context, "Laporan ditandai sebagai Spam/Palsu", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDeleteItem = { itemId ->
                            scope.launch {
                                itemRepo.deleteReport(itemId)
                                Toast.makeText(context, "Laporan berhasil dihapus", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onBack = { currentScreen = "home" },
                        onItemClick = { item -> inspectedItem = item }
                    )
                }

                "notifications" -> {
                    NotificationsScreen(
                        notifications = notificationsState,
                        onNotificationClick = { notif ->
                            scope.launch {
                                notifRepo.markAsRead(notif.id)
                                if (notif.itemId.isNotBlank()) {
                                    val matchedItem = itemsState.find { it.id == notif.itemId }
                                    if (matchedItem != null) {
                                        inspectedItem = matchedItem
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    // Inspect Item Dialog
    inspectedItem?.let { item ->
        ItemDetailDialog(
            item = item,
            currentUserId = user.uid,
            isAdmin = isAdmin,
            onDismiss = { inspectedItem = null },
            onStartChat = { targetItem ->
                scope.launch {
                    val result = chatRepo.getOrCreateChat(
                        lostUserId = if (targetItem.type == "LOST") targetItem.userId else user.uid,
                        foundUserId = if (targetItem.type == "FOUND") targetItem.userId else user.uid,
                        itemId = targetItem.id,
                        itemTitle = targetItem.title
                    )
                    result.onSuccess { chatId ->
                        activeChatRoom = ChatRoom(
                            id = chatId,
                            itemId = targetItem.id,
                            itemTitle = targetItem.title,
                            participantUids = listOf(user.uid, targetItem.userId)
                        )
                    }
                }
            },
            onMarkResolved = { itemId ->
                scope.launch {
                    itemRepo.markResolved(itemId)
                    Toast.makeText(context, "Status diperbarui menjadi Selesai!", Toast.LENGTH_SHORT).show()
                }
            },
            onDelete = { itemId ->
                scope.launch {
                    itemRepo.deleteReport(itemId)
                    Toast.makeText(context, "Laporan berhasil dihapus.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}
