package com.suseoaa.projectoaa.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.app.AppNavHost
import com.suseoaa.projectoaa.feature.home.component.NavigationItemForPad

// 定义 Tab 的顺序和元数据
enum class MainTab(
    val index: Int,
    val icon: ImageVector,
    val label: String
) {
    HOME(0, Icons.Default.Home, "首页"),
    COURSE(1, Icons.Default.Book, "课程"),
    ACADEMIC(2, Icons.Default.ChatBubble, "教务信息"),
    PERSON(3, Icons.Default.Person, "个人");

    companion object {
        fun getByIndex(index: Int): MainTab = entries.getOrElse(index) { HOME }
    }
}

@Composable
fun OaaApp(
    windowSizeClass: WindowWidthSizeClass,
    startDestination: String
) {
    // 这里的 navController 是整个 App 的根控制器（用于 Login -> Main 的跳转）
    val navController = androidx.navigation.compose.rememberNavController()

    AppNavHost(
        navController = navController,
        windowSizeClass = windowSizeClass,
        startDestination = startDestination
    )
}

// 手机端底部栏：现在接收 selectedIndex
@Composable
fun OaaBottomBar(
    selectedIndex: Int,
    onNavigate: (Int) -> Unit,
    modifier: Modifier
) {
    NavigationBar(
        modifier = modifier // [修改点 2] 应用传入的 modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        containerColor = Color.Transparent, // 确保外层透明
        tonalElevation = 0.dp // 去除 Material3 默认的色调
    ) {
        Surface(
            tonalElevation = 2.dp,
            shadowElevation = 1.dp,
            modifier = Modifier.padding(10.dp),
            shape = RoundedCornerShape(26.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MainTab.entries.forEach { tab ->
                    val isSelected = selectedIndex == tab.index
                    NavigationRailItem(
                        selected = isSelected,
                        onClick = { onNavigate(tab.index) },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    }
}

// 平板端侧边栏：现在接收 selectedIndex
@Composable
fun OaaNavRail(
    selectedIndex: Int,
    onNavigate: (Int) -> Unit
) {
    NavigationRail {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.2f)
        ) {
            MainTab.entries.forEach { tab ->
                val isSelected = selectedIndex == tab.index
                NavigationItemForPad(
                    selected = isSelected,
                    onClick = { onNavigate(tab.index) },
                    icon = tab.icon,
                    label = tab.label
                )
            }
        }
    }
}