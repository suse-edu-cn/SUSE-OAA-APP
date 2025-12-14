import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

// 1. 定义路由枚举
enum class AppRoute(val route: String) {
    HOME("home"),
    PROFILE("profile"),
    SETTINGS("settings")
}

// 2. 定义底部/侧边栏的菜单项数据模型
data class NavigationItem(
    val title: String,
    val icon: ImageVector,
    val route: AppRoute
)

// 3. 静态配置菜单列表
val topLevelDestinations = listOf(
    NavigationItem("首页", Icons.Default.Home, AppRoute.HOME),
    NavigationItem("我的", Icons.Default.Person, AppRoute.PROFILE),
    NavigationItem("设置", Icons.Default.Settings, AppRoute.SETTINGS)
)