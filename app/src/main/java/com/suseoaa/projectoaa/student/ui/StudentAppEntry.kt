package com.suseoaa.projectoaa.student.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
// [修复] 移除了 LocalContext 导入
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
// [修复] 导入 Hilt 和 ViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.suseoaa.projectoaa.navigation.viewmodel.ShareViewModel
import com.suseoaa.projectoaa.student.viewmodel.StudentFormViewModel
// [解耦] 移除了 ThemeManager 导入
// [修复] 移除了静态 SessionManager 和 WallpaperManager 导入
import com.suseoaa.projectoaa.login.ui.ProfileScreen
import kotlinx.coroutines.launch

enum class AppScreen { Start, Form, Profile }

@Composable
fun StudentAppMainEntry(onLogout: () -> Unit = {}) {
    var currentScreen by remember { mutableStateOf(AppScreen.Start) }

    // [修复] 1. 获取 ShareViewModel
    val shareViewModel: ShareViewModel = hiltViewModel()
    // [修复] 2. 从 ViewModel 读取主题
    val currentThemeName = shareViewModel.currentTheme.name

    when (currentScreen) {
        AppScreen.Start -> {
            StartSelectionScreen(
                // [修复] 3. 传递 ViewModel
                shareViewModel = shareViewModel,
                onStartClick = { currentScreen = AppScreen.Form },
                onProfileClick = { currentScreen = AppScreen.Profile } ,
            )
        }

        AppScreen.Form -> {
            BackHandler { currentScreen = AppScreen.Start }
            StudentFormScreen(
                // [修复] 4. StudentFormScreen 使用 Hilt 获取自己的 ViewModel
                viewModel = hiltViewModel<StudentFormViewModel>(),
                onBack = { currentScreen = AppScreen.Start },
                currentThemeName = currentThemeName
            )
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
fun StartSelectionScreen(
    // [修复] 5. 接收 ShareViewModel
    shareViewModel: ShareViewModel,
    onStartClick: () -> Unit,
    onProfileClick: () -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showThemeDialog by remember { mutableStateOf(false) }
    // [修复] 6. 移除了 context

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
                        // [修复] 7. 从 ViewModel 读取用户信息
                        Text(
                            text = shareViewModel.currentUser ?: "User",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = shareViewModel.currentRole ?: "Role",
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
                // [修复] 8. 从 ViewModel 读取主题
                if (shareViewModel.currentTheme.name.contains("二次元")) {
                    NavigationDrawerItem(
                        label = { Text("获取当前壁纸") },
                        icon = { Icon(Icons.Default.Image, null) },
                        selected = false,
                        // [修复] 9. 调用 ViewModel 的方法
                        onClick = { shareViewModel.onSaveWallpaper(); scope.launch { drawerState.close() } },
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
                            // [修复] 10. 从 ViewModel 读取用户信息
                            Text(
                                "欢迎回来，${shareViewModel.currentUser}",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "身份：${shareViewModel.currentRole}",
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
                        // [修复] 11. 从 ViewModel 读取用户信息
                        Text(
                            "欢迎回来，${shareViewModel.currentUser}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "身份：${shareViewModel.currentRole}",
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

    // [修复] 解决了错误并完成了与 ViewModel 的解耦
    if (showThemeDialog) {
        ThemeSelectionDialog(
            onDismiss = { showThemeDialog = false },
            onThemeSelected = { theme ->
                shareViewModel.updateTheme(theme)
                showThemeDialog = false
            }
        )
    }
}