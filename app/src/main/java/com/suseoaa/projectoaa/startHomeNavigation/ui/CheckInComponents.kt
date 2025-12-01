package com.suseoaa.projectoaa.navigation.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.suseoaa.projectoaa.navigation.viewmodel.CheckInUiState
import com.suseoaa.projectoaa.navigation.viewmodel.CheckInViewModel
import com.suseoaa.projectoaa.navigation.viewmodel.HomeUiState

/**
 * 封装了打卡功能的 "Smart" Composable。
 */
@Composable
fun CheckInCard(
    homeUiState: HomeUiState, // 接收来自 HomeViewModel 的日期/倒计时
    primaryColor: Color,
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color,
    viewModel: CheckInViewModel = hiltViewModel()
) {
    val checkInUiState = viewModel.uiState

    CheckInCardContent(
        checkInUiState = checkInUiState,
        homeUiState = homeUiState,
        onCheckIn = viewModel::onCheckIn,
        primaryColor = primaryColor,
        onSurfaceColor = onSurfaceColor,
        onSurfaceVariantColor = onSurfaceVariantColor
    )
}

/**
 * 纯粹用于渲染卡片 UI 的 "Dumb" Composable。
 */
@Composable
private fun CheckInCardContent(
    checkInUiState: CheckInUiState,
    homeUiState: HomeUiState,
    onCheckIn: () -> Unit,
    primaryColor: Color,
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp), // 整体高度不变
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val context = LocalContext.current
            val imageUrl = checkInUiState.placeholderImageUrl

            // 构建 ImageRequest 以禁用缓存
            val imageRequest = remember(imageUrl) {
                if (imageUrl == null) {
                    null
                } else {
                    ImageRequest.Builder(context)
                        .data(imageUrl)
                        .memoryCachePolicy(CachePolicy.DISABLED)
                        .diskCachePolicy(CachePolicy.DISABLED)
                        .build()
                }
            }

            if (imageRequest != null) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = "每日打卡封面",
                    alignment = Alignment.TopCenter,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    onError = { android.util.Log.e("CheckInCard", "Image load failed: ${it.result.throwable}") },
                    onSuccess = { android.util.Log.d("CheckInCard", "Image load success") }
                )
            }

            // 渐变遮罩层
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
            )

            // 内容层
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 上半部分 (签文)
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    Crossfade(targetState = checkInUiState.isCheckedIn, label = "Fortune") { isChecked ->
                        if (isChecked) {
                            FortuneContent(
                                checkInCount = checkInUiState.checkInCount,
                                cspCountdown = homeUiState.cspCountdown,
                                noipCountdown = homeUiState.noipCountdown,
                                primaryColor, onSurfaceColor, onSurfaceVariantColor
                            )
                        } else {
                            Box(Modifier.fillMaxSize())
                        }
                    }
                }
                // 下半部分 (操作)
                Box(modifier = Modifier.fillMaxWidth()) {
                    Crossfade(targetState = checkInUiState.isCheckedIn, label = "Action") { isChecked ->
                        if (isChecked) {
                            AfterCheckInInfo()
                        } else {
                            BeforeCheckInInfo(
                                currentDate = homeUiState.currentDate,
                                cspCountdown = homeUiState.cspCountdown,
                                noipCountdown = homeUiState.noipCountdown,
                                onCheckIn = onCheckIn,
                                onSurfaceColor, onSurfaceVariantColor
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 辅助组件
// ==========================================

@Composable
private fun FortuneContent(
    checkInCount: Int,
    cspCountdown: String,
    noipCountdown: String,
    primaryColor: Color,
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color
) {
    // 强制此区域内的字体不缩放 (fontScale = 1.0f)
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = LocalDensity.current.density,
            fontScale = 1.0f
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                .padding(vertical = 8.dp, horizontal = 20.dp),
        ) {
            // --- 顶部信息 (中吉, 连续打卡) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    "中吉",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "连续打卡",
                        fontSize = 14.sp,
                        color = onSurfaceVariantColor
                    )
                    Text(
                        text = "$checkInCount",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        lineHeight = 40.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        cspCountdown,
                        color = onSurfaceVariantColor,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        noipCountdown,
                        color = onSurfaceVariantColor,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.height(16.dp)) // [修复] 顶部和标题行之间的间隔

            // --- [修复] “宜”和“忌”的新布局 ---

            // 1. 标题行
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    FortuneHeader(isGood = true) // "宜"
                }
                Box(modifier = Modifier.weight(1f)) {
                    FortuneHeader(isGood = false) // "忌"
                }
            }

            Spacer(Modifier.height(4.dp)) // 标题和条目之间的间隔

            // 2. [修复] 所有 4 个条目在同一行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp) // 条目间的最小间隔
            ) {
                // "宜" 的条目
                FortuneItem(
                    title = "刷算法",
                    subtitle = "AC+10",
                    onSurfaceColor,
                    onSurfaceVariantColor,
                    modifier = Modifier.weight(1f) // Item 1
                )
                FortuneItem(
                    title = "写文档",
                    subtitle = "思如泉涌",
                    onSurfaceColor,
                    onSurfaceVariantColor,
                    modifier = Modifier.weight(1f) // Item 2
                )

                // "忌" 的条目
                FortuneItem(
                    title = "重构代码",
                    subtitle = "Bug++",
                    onSurfaceColor,
                    onSurfaceVariantColor,
                    modifier = Modifier.weight(1f) // Item 3
                )
                FortuneItem(
                    title = "熬夜",
                    subtitle = "头发--",
                    onSurfaceColor,
                    onSurfaceVariantColor,
                    modifier = Modifier.weight(1f) // Item 4
                )
            }
        }
    }
}

@Composable
private fun FortuneHeader(isGood: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = if (isGood) "宜" else "忌",
            color = if (isGood) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(
                    if (isGood) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                    CircleShape
                )
                .padding(horizontal = 10.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun FortuneItem(
    title: String,
    subtitle: String,
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            title,
            color = onSurfaceColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            subtitle,
            color = onSurfaceVariantColor,
            fontSize = 10.sp // [修复] 减小副标题字体大小以适应压缩
        )
    }
}

@Composable
private fun BeforeCheckInInfo(
    currentDate: String,
    cspCountdown: String,
    noipCountdown: String,
    onCheckIn: () -> Unit,
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(currentDate, color = onSurfaceColor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(cspCountdown, color = onSurfaceVariantColor, style = MaterialTheme.typography.bodyMedium)
        Text(noipCountdown, color = onSurfaceVariantColor, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onCheckIn,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("立即打卡", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AfterCheckInInfo() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Button(
            onClick = { /* TODO: 实现分享逻辑 */ },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("分享欧气")
        }
    }
}