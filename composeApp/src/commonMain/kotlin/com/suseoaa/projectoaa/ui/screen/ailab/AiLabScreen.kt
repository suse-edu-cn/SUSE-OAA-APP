@file:Suppress("UnusedAssignment")

package com.suseoaa.projectoaa.ui.screen.ailab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suseoaa.projectoaa.presentation.ailab.AiLabUiState
import com.suseoaa.projectoaa.presentation.ailab.AiLabViewModel
import com.suseoaa.projectoaa.presentation.ailab.ModelDownloadState
import com.suseoaa.projectoaa.util.DeviceInfo
import com.suseoaa.projectoaa.util.ModelRecommendation
import com.suseoaa.projectoaa.util.ModelRecommendationLevel
import com.suseoaa.projectoaa.util.toReadableStorage
import com.suseoaa.projectoaa.util.ToastManager
import org.koin.compose.viewmodel.koinViewModel

import androidx.compose.material.icons.filled.Folder
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.filled.Folder
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.window.DialogProperties
import com.suseoaa.projectoaa.util.LocalModelFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiLabScreen(
    onBack: () -> Unit,
    onNavigateToAcademicAnalysis: () -> Unit = {},
    onNavigateToAiChat: () -> Unit = {},
    viewModel: AiLabViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val showTokenDialog by viewModel.showTokenDialog.collectAsState()
    var showModelManagerDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadLocalModels()
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            ToastManager.showToast(msg)
            viewModel.clearError()
        }
    }
    
    if (showTokenDialog) {
        var inputUsername by remember { mutableStateOf("") }
        var inputKey by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.dismissTokenDialog() },
            title = { Text("需要 Kaggle API 凭证", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "为了获取最高精度和原生的模型资源，我们已切换至 Kaggle 官方源。请在 Kaggle 个人设置中生成 API Key (kaggle.json)，并在此填入。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = inputUsername,
                        onValueChange = { inputUsername = it },
                        label = { Text("Kaggle Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inputKey,
                        onValueChange = { inputKey = it },
                        label = { Text("Kaggle API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.submitKaggleAuthAndDownload(inputUsername.trim(), inputKey.trim()) }
                ) {
                    Text("保存并下载")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissTokenDialog() }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("AI 实验室", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        viewModel.loadLocalModels()
                        showModelManagerDialog = true 
                    }) {
                        Icon(Icons.Default.Folder, contentDescription = "模型管理")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp,
                vertical = 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 分区 1：设备能力检测 ────────────────────────────────────
            item {
                DeviceCapabilitySection(
                    isLoading = uiState.isLoadingDeviceInfo,
                    deviceInfo = uiState.deviceInfo
                )
            }

            // ── 分区 2：模型推荐 ──────────────────────────────────────
            item {
                AnimatedVisibility(
                    visible = !uiState.isLoadingDeviceInfo,
                    enter = fadeIn(tween(400)) + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    uiState.recommendation?.let { rec ->
                        ModelRecommendationSection(
                            recommendation = rec,
                            selectedModel = uiState.selectedModel,
                            availableModels = uiState.availableModels,
                            downloadState = uiState.downloadState,
                            hasUpdateAvailable = uiState.hasUpdateAvailable,
                            onSelectModel = { viewModel.selectModel(it) },
                            onDownload = { viewModel.startDownload() },
                            onCancelDownload = { viewModel.cancelDownload() }
                        )
                    }
                }
            }

            // ── 分区 3：AI 功能入口 ───────────────────────────────────
            item {
                AiFeaturesSection(
                    isModelAvailable = uiState.downloadState is ModelDownloadState.Downloaded,
                    onNavigateToAcademicAnalysis = onNavigateToAcademicAnalysis,
                    onNavigateToAiChat = onNavigateToAiChat
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showModelManagerDialog) {
        ModelManagerDialog(
            localModels = uiState.localModels,
            onDismiss = { showModelManagerDialog = false },
            onDelete = { fileName -> viewModel.deleteLocalModel(fileName) }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 分区 1：设备能力检测面板
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DeviceCapabilitySection(
    isLoading: Boolean,
    deviceInfo: DeviceInfo?
) {
    SectionLabel(text = "设备能力检测")

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "正在检测设备能力…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (deviceInfo != null) {
            val totalRamGb = deviceInfo.totalRam / (1024f * 1024f * 1024f)
            val availRamGb = deviceInfo.availableRam / (1024f * 1024f * 1024f)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // RAM 使用率进度条
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "内存 (RAM)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "可用 ${"%.1f".format(availRamGb)}GB / 共 ${"%.1f".format(totalRamGb)}GB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    val usedFraction = if (totalRamGb > 0) (1f - availRamGb / totalRamGb).coerceIn(0f, 1f) else 0f
                    val ramBarColor = when {
                        usedFraction > 0.85f -> Color(0xFFFF3B30)
                        usedFraction > 0.65f -> Color(0xFFFF9500)
                        else -> MaterialTheme.colorScheme.primary
                    }
                    LinearProgressIndicator(
                        progress = { usedFraction },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = ramBarColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                // 参数网格
                val items = buildHardwareItems(deviceInfo)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            row.forEach { item ->
                                HardwareParamChip(
                                    label = item.first,
                                    value = item.second,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // 如果这行只有 1 个，补一个空 weight
                            if (row.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "无法读取设备信息",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun buildHardwareItems(info: DeviceInfo): List<Pair<String, String>> {
    val totalRamGb = info.totalRam / (1024f * 1024f * 1024f)
    return buildList {
        add("CPU 型号" to info.cpuModel.take(28))
        add("GPU 渲染器" to info.gpuRenderer.take(28))
        add("NPU 支持" to if (info.hasNpu) info.npuDescription.take(24) else "未检测到")
        add("总内存" to "${"%.1f".format(totalRamGb)} GB")
        add("总存储" to info.totalStorage.toReadableStorage())
        add("可用存储" to info.availableStorage.toReadableStorage())
        add("系统版本" to info.osVersion)
    }
}

@Composable
private fun HardwareParamChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 分区 2：模型推荐与下载
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelRecommendationSection(
    recommendation: ModelRecommendation,
    selectedModel: com.suseoaa.projectoaa.util.AiModelMetadata?,
    availableModels: List<com.suseoaa.projectoaa.util.AiModelMetadata>,
    downloadState: ModelDownloadState,
    hasUpdateAvailable: Boolean,
    onSelectModel: (String) -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit
) {
    SectionLabel(text = "推荐模型")

    val isSupported = recommendation.level != ModelRecommendationLevel.NOT_RECOMMENDED

    val gradientColors = when (recommendation.level) {
        ModelRecommendationLevel.E4B_RECOMMENDED -> listOf(
            Color(0xFF1A237E), Color(0xFF283593)
        )
        ModelRecommendationLevel.E2B_RECOMMENDED -> listOf(
            Color(0xFF00695C), Color(0xFF00796B)
        )
        ModelRecommendationLevel.NOT_RECOMMENDED -> listOf(
            Color(0xFF37474F), Color(0xFF455A64)
        )
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = gradientColors,
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                // 标题行
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Psychology,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        var expanded by remember { mutableStateOf(false) }
                        
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.menuAnchor()
                            ) {
                                Text(
                                    text = selectedModel?.name ?: recommendation.modelName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Switch Model",
                                    tint = Color.White
                                )
                            }
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                availableModels.forEach { model ->
                                    DropdownMenuItem(
                                        text = { Text(model.name) },
                                        onClick = { 
                                            onSelectModel(model.id)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Text(
                            text = selectedModel?.sizeDesc ?: recommendation.modelSizeDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    // 适配徽章
                    val (badgeColor, badgeText) = when (recommendation.level) {
                        ModelRecommendationLevel.E4B_RECOMMENDED -> Color(0xFF69F0AE) to "完整体验"
                        ModelRecommendationLevel.E2B_RECOMMENDED -> Color(0xFF40C4FF) to "推荐"
                        ModelRecommendationLevel.NOT_RECOMMENDED -> Color(0xFFFF5252) to "不支持"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(badgeColor.copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 推荐原因
                // 推荐原因或警告
                val reasonText = if (selectedModel != null && selectedModel.recommendedLevel != recommendation.level) {
                    "⚠️ 警告：您选择的模型非系统推荐模型，强行运行可能导致内存溢出或闪退。"
                } else {
                    recommendation.reason
                }
                
                Text(
                    text = reasonText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selectedModel != null && selectedModel.recommendedLevel != recommendation.level) Color(0xFFFFD740) else Color.White.copy(alpha = 0.85f),
                    lineHeight = 18.sp
                )

                // 下载状态
                if (isSupported) {
                    Spacer(modifier = Modifier.height(16.dp))
                    when (downloadState) {
                        is ModelDownloadState.Idle -> {
                            Surface(
                                onClick = onDownload,
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Storage,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "开始下载（${selectedModel?.sizeDesc ?: recommendation.modelSizeDesc}）",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        is ModelDownloadState.NotOnWifi -> {
                            Text(
                                text = "⚠️ 请连接 Wi-Fi 后再下载，避免消耗流量",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFFD740)
                            )
                        }
                        is ModelDownloadState.Downloading -> {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "正在下载… ${downloadState.speedStr}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${(downloadState.progress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { downloadState.progress },
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                                    color = Color.White,
                                    trackColor = Color.White.copy(alpha = 0.25f),
                                    strokeCap = StrokeCap.Round
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${downloadState.downloadedBytes.toReadableStorage()} / ${downloadState.totalBytes.toReadableStorage()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.65f)
                                    )
                                    Surface(
                                        onClick = onCancelDownload,
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFFF5252).copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "取消下载",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFFF5252)
                                        )
                                    }
                                }
                            }
                        }
                        is ModelDownloadState.Downloaded -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF69F0AE))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "模型已就绪，AI 功能已启用",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF69F0AE)
                                    )
                                }
                                
                                if (hasUpdateAvailable) {
                                    Surface(
                                        onClick = onDownload,
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF2196F3).copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "官方有更新 (重新下载)",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF64B5F6)
                                        )
                                    }
                                }
                            }
                        }
                        is ModelDownloadState.Error -> {
                            Text(
                                text = "下载失败：${downloadState.message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFF5252)
                            )
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 分区 3：AI 功能入口列表
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AiFeaturesSection(
    isModelAvailable: Boolean,
    onNavigateToAcademicAnalysis: () -> Unit,
    onNavigateToAiChat: () -> Unit
) {
    SectionLabel(text = "AI 功能")

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AiFeatureRow(
                icon = Icons.Default.Summarize,
                title = "调课通知摘要",
                subtitle = if (isModelAvailable) "模型已就绪，将在拉取调课通知时自动摘要" else "下载模型后启用 · 长文本一键提炼关键信息",
                isEnabled = isModelAvailable,
                onClick = null // 纯展示，功能在通知页自动生效
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 80.dp, end = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
            AiFeatureRow(
                icon = Icons.Default.Analytics,
                title = "学业规划分析",
                subtitle = if (isModelAvailable) "分析毕业差距与目标绩点路径（含精确计算工具）" else "下载模型后启用 · 结合成绩与培养方案深度分析",
                isEnabled = isModelAvailable,
                onClick = {
                    if (isModelAvailable) {
                        onNavigateToAcademicAnalysis()
                    }
                }
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 80.dp, end = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
            AiFeatureRow(
                icon = Icons.Default.AutoAwesome,
                title = "本地自由对话",
                subtitle = if (isModelAvailable) "开启思维链深思模式的纯离线自由对话" else "下载模型后启用真 AI 对话",
                isEnabled = isModelAvailable,
                onClick = {
                    if (isModelAvailable) {
                        onNavigateToAiChat()
                    }
                }
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 80.dp, end = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
            AiFeatureRow(
                icon = Icons.Default.Search,
                title = "智能数据查询",
                subtitle = if (isModelAvailable) "用自然语言搜索所有历史数据" else "需下载模型 · 仅限高级模型",
                isEnabled = isModelAvailable,
                onClick = {
                    if (isModelAvailable) {
                        onNavigateToAiChat()
                    }
                }
            )
        }
    }

    if (!isModelAvailable) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "📝  下载并载入模型后，以上功能将自动解锁。模型仅在设备本地运行，数据绝不离开您的手机。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun AiFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isEnabled: Boolean,
    onClick: (() -> Unit)?
) {
    val contentAlpha = if (isEnabled) 1f else 0.55f

    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isEnabled)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = (if (isEnabled)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = contentAlpha),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                    lineHeight = 16.sp
                )
            }
            if (!isEnabled) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "未启用",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (onClick != null && isEnabled) {
        Surface(
            onClick = onClick,
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) { content() }
    } else {
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 工具组件
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
fun ModelManagerDialog(
    localModels: List<LocalModelFile>,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "本地模型管理",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (localModels.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无下载的本地模型文件",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(localModels) { file ->
                            val sizeGb = file.sizeBytes.toDouble() / (1024 * 1024 * 1024)
                            val sizeStr = ((sizeGb * 100).toInt() / 100.0).toString() + " GB"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = sizeStr,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { onDelete(file.name) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "删除",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("关闭")
                    }
                }
            }
        }
    }
}
