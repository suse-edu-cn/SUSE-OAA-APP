package com.suseoaa.projectoaa.student.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.common.theme.ThemeManager
import com.suseoaa.projectoaa.common.util.SessionManager
import com.suseoaa.projectoaa.common.util.WallpaperManager
import com.suseoaa.projectoaa.login.ui.ProfileScreen
import kotlinx.coroutines.launch

enum class AppScreen { Start, Form, Profile }

@Composable
fun StudentAppMainEntry(onLogout: () -> Unit = {}) {
    var currentScreen by remember { mutableStateOf(AppScreen.Start) }

    when (currentScreen) {
        AppScreen.Start -> {
            StartSelectionScreen(
                onStartClick = { currentScreen = AppScreen.Form },
                onProfileClick = { currentScreen = AppScreen.Profile }
            )
        }

        AppScreen.Form -> {
            BackHandler { currentScreen = AppScreen.Start }
            StudentFormScreen(onBack = { currentScreen = AppScreen.Start })
        }

        AppScreen.Profile -> {
            BackHandler { currentScreen = AppScreen.Start }
            ProfileScreen(
                onBack = { currentScreen = AppScreen.Start },
                onLogout = onLogout
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartSelectionScreen(onStartClick: () -> Unit, onProfileClick: () -> Unit) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showThemeDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = SessionManager.currentUser ?: "User",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = SessionManager.currentRole ?: "Role",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                NavigationDrawerItem(
                    label = { Text("个人资料") },
                    icon = { Icon(Icons.Default.Person, null) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close(); onProfileClick() } },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                if (ThemeManager.currentTheme.name.contains("二次元")) {
                    NavigationDrawerItem(
                        label = { Text("获取当前壁纸") },
                        icon = { Icon(Icons.Default.Image, null) },
                        selected = false,
                        onClick = { WallpaperManager.saveCurrentToGallery(context); scope.launch { drawerState.close() } },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
                NavigationDrawerItem(
                    label = { Text("关于 Project OAA") },
                    icon = { Icon(Icons.Default.Info, null) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Project:OAA") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                Icons.Default.Menu,
                                "菜单",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            showThemeDialog = true
                        }) {
                            Icon(
                                Icons.Default.Settings,
                                "切换主题",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                val isWide = maxWidth > 600.dp && maxWidth > maxHeight
                if (isWide) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "欢迎回来，${SessionManager.currentUser}",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "身份：${SessionManager.currentRole}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Spacer(modifier = Modifier.width(48.dp))
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            LargeSelectionButton(text = "招新/换届申请", onClick = onStartClick)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "欢迎回来，${SessionManager.currentUser}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "身份：${SessionManager.currentRole}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(64.dp))
                        LargeSelectionButton(text = "招新/换届申请", onClick = onStartClick)
                    }
                }
            }
        }
    }
    if (showThemeDialog) ThemeSelectionDialog { showThemeDialog = false }
}