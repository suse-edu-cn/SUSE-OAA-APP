package com.suseoaa.projectoaa.startHomeNavigation.ui

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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.suseoaa.projectoaa.startHomeNavigation.viewmodel.CheckInUiState
import com.suseoaa.projectoaa.startHomeNavigation.viewmodel.CheckInViewModel
import com.suseoaa.projectoaa.startHomeNavigation.viewmodel.DailyFortune
import com.suseoaa.projectoaa.startHomeNavigation.viewmodel.FortuneItem
import com.suseoaa.projectoaa.startHomeNavigation.viewmodel.HomeUiState

/**
 * 封装了打卡功能的 "Smart" Composable。
 */
@Composable
fun CheckInCard(
    homeUiState: HomeUiState,
    primaryColor: Color,
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color,
    viewModel: CheckInViewModel = hiltViewModel()
) {
    val checkInUiState = viewModel.uiState

    CheckInCardContent(
        checkInUiState = checkInUiState,
        isLoading = checkInUiState.isLoading,
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
    isLoading: Boolean,
    homeUiState: HomeUiState,
    onCheckIn: () -> Unit,
    primaryColor: Color,
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = primaryColor,
                    modifier = Modifier.size(48.dp)
                )
            }
        } else {
            CheckInCardLoadedContent(
                checkInUiState = checkInUiState,
                homeUiState = homeUiState,
                onCheckIn = onCheckIn,
                primaryColor = primaryColor,
                onSurfaceColor = onSurfaceColor,
                onSurfaceVariantColor = onSurfaceVariantColor
            )
        }
    }
}

/**
 * 主要内容区域
 */
@Composable
private fun CheckInCardLoadedContent(
    checkInUiState: CheckInUiState,
    homeUiState: HomeUiState,
    onCheckIn: () -> Unit,
    primaryColor: Color,
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        val context = LocalContext.current
        val imageUrl = checkInUiState.placeholderImageUrl

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
                onError = { android.util.Log.e("CheckInCard", "Image load failed: ${it.result.throwable}") }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                Crossfade(targetState = checkInUiState.isCheckedIn, label = "Fortune") { isChecked ->
                    if (isChecked) {
                        FortuneContent(
                            checkInCount = checkInUiState.checkInCount,
                            cspCountdown = homeUiState.cspCountdown,
                            noipCountdown = homeUiState.noipCountdown,
                            dailyFortune = checkInUiState.dailyFortune,
                            primaryColor, onSurfaceColor, onSurfaceVariantColor
                        )
                    } else {
                        Box(Modifier.fillMaxSize())
                    }
                }
            }
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

// ==========================================
// 运势展示逻辑 (核心修改区域)
// ==========================================

@Composable
private fun FortuneContent(
    checkInCount: Int,
    cspCountdown: String,
    noipCountdown: String,
    dailyFortune: DailyFortune?,
    primaryColor: Color,
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color
) {
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
            // --- 顶部信息 ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = dailyFortune?.luckLevel ?: "计算中...",
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
                    Text(cspCountdown, color = onSurfaceVariantColor, style = MaterialTheme.typography.bodySmall)
                    Text(noipCountdown, color = onSurfaceVariantColor, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(20.dp))

            // --- 宜忌详情区域 (4列2行布局) ---
            if (dailyFortune != null) {
                // 固定显示2行
                val rows = 2

                Column(modifier = Modifier.fillMaxWidth()) {
                    for (i in 0 until rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // -------------------------
                            // 第一组：宜 (Col 1 & Col 2)
                            // -------------------------

                            // Col 1: 宜 Header (固定宽度占位，确保上下对齐)
                            Box(
                                modifier = Modifier.width(42.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                // 仅在第一行显示图标，保持页面清爽
                                if (i == 0) FortuneHeader(isGood = true)
                            }

                            // Col 2: 宜 内容
                            Box(modifier = Modifier.weight(1f)) {
                                if (dailyFortune.goodList.isEmpty()) {
                                    // 列表为空 -> 诸事不宜
                                    if (i == 0) {
                                        SpecialFortuneText(
                                            text = "诸事不宜",
                                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                        )
                                    }
                                } else {
                                    val item = dailyFortune.goodList.getOrNull(i)
                                    if (item != null) {
                                        FortuneItemView(item, onSurfaceColor, onSurfaceVariantColor)
                                    }
                                }
                            }

                            // 组间距
                            Spacer(Modifier.width(12.dp))

                            // -------------------------
                            // 第二组：忌 (Col 3 & Col 4)
                            // -------------------------

                            // Col 3: 忌 Header (固定宽度占位)
                            Box(
                                modifier = Modifier.width(42.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (i == 0) FortuneHeader(isGood = false)
                            }

                            // Col 4: 忌 内容
                            Box(modifier = Modifier.weight(1f)) {
                                if (dailyFortune.badList.isEmpty()) {
                                    // 列表为空 -> 万事皆宜
                                    if (i == 0) {
                                        SpecialFortuneText(
                                            text = "万事皆宜",
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else {
                                    val item = dailyFortune.badList.getOrNull(i)
                                    if (item != null) {
                                        FortuneItemView(item, onSurfaceColor, onSurfaceVariantColor)
                                    }
                                }
                            }
                        }

                        // 行间距
                        if (i < rows - 1) {
                            Spacer(Modifier.height(12.dp))
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
private fun FortuneItemView(
    item: FortuneItem,
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            item.title,
            color = onSurfaceColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Text(
            item.subtitle,
            color = onSurfaceVariantColor,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun SpecialFortuneText(text: String, color: Color) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 18.sp, // 稍微调大一点点，但不至于破坏布局
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic
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