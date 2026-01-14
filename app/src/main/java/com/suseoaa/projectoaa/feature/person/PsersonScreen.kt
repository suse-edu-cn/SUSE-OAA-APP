package com.suseoaa.projectoaa.feature.person

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.suseoaa.projectoaa.app.LocalWindowSizeClass
import com.suseoaa.projectoaa.core.network.model.person.Data

// 定义头图高度
private val HeaderHeight = 320.dp

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonScreen(
    viewModel: PersonViewModel = hiltViewModel()
) {
    val userInfo by viewModel.userInfo.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.errorMessage.collectAsStateWithLifecycle()

    val windowSizeClass = LocalWindowSizeClass.current
    // 手机端单列(看起来像列表)，平板端双列或多列
    val isPhone = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
    val gridColumns = if (isPhone) 1 else 2
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // 获取底部导航栏高度 (因为 MainScreen 取消了 padding，这里可能也需要处理底部遮挡，
    // 但通常 BottomBar 是浮动的，LazyVerticalGrid 只需要加 contentPadding 即可)
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // 1. 滚动状态用于驱动动画
    val scrollState = rememberLazyGridState()
    val density = LocalDensity.current
    val headerHeightPx = with(density) { HeaderHeight.toPx() }

    Scaffold(
        // 将背景设为透明，以便看到我们自定义的底层
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // 2. 底层：动态背景
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HeaderHeight)
                    .graphicsLayer {
                        // 计算滚动偏移量
                        val scrollOffset = scrollState.firstVisibleItemScrollOffset
                        val firstVisibleIndex = scrollState.firstVisibleItemIndex

                        // 只有当第一个 Item (Spacer) 可见时才应用视差
                        if (firstVisibleIndex == 0) {
                            // Parallax: 背景移动速度是前景的一半 (0.5f)
                            translationY = -scrollOffset * 0.5f
                            // Scale: 稍微缩小，模拟“下沉”效果
                            val scale = 1f - (scrollOffset / headerHeightPx) * 0.1f
                            scaleX = scale.coerceAtLeast(0.9f)
                            scaleY = scale.coerceAtLeast(0.9f)
                            // Alpha: 慢慢变淡
                            alpha = (1f - (scrollOffset / headerHeightPx) * 1.5f).coerceIn(0f, 1f)
                        } else {
                            // 如果滑得太远，直接隐藏背景避免遮挡
                            alpha = 0f
                        }
                    }
                    .background(
                        // 模仿 HyperOS 的渐变色
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF9BDCE5),
                                Color(0xFF8EC5FC),
                                MaterialTheme.colorScheme.background // 底部渐变融合
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // 背景中的大标题
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "青蟹",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "致力服务于四川轻化工大学开放原子开放协会",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black.copy(alpha = 0.5f)
                    )
                }
            }

            // 3. 顶层：滚动内容
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    state = scrollState,
                    contentPadding = PaddingValues(
                        top = 16.dp + statusBarHeight, // 让内容避开状态栏
                        bottom = 16.dp + navBarHeight + 80.dp, // 底部避开导航栏和 BottomBar
                        start = 16.dp,
                        end = 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Item 0: 透明占位符，高度等于头图，让背景露出来
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Spacer(modifier = Modifier.height(HeaderHeight - 80.dp)) // 稍微留一点重叠
                    }

                    // Item 1: 用户信息卡片 (跨满整行)
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        UserInfoCard(userInfo)
                    }

                    // Item 2...N: 功能卡片/设备信息
                    item(span = { GridItemSpan(if (isPhone) maxLineSpan else 1) }) {
                        InfoItemCard(title = "信息1", value = "1")
                    }
                    item(span = { GridItemSpan(if (isPhone) maxLineSpan else 1) }) {
                        InfoItemCard(title = "信息2", value = "2")
                    }

                    // 更多设置项模拟
                    items(5) { index ->
                        InfoItemCard(title = "更多设置 $index", value = "已开启")
                    }

                    // 底部留白
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

// 组件封装

@Composable
fun UserInfoCard(userInfo: Data?) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp), // 大圆角
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像
            AsyncImage(
                model = userInfo?.avatar ?: "",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userInfo?.name ?: "请登录",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = userInfo?.department ?: "暂未加入任何部门",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = userInfo?.role ?: "查看会员权益",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun InfoItemCard(title: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}