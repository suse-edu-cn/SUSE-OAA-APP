package com.suseoaa.projectoaa.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suseoaa.projectoaa.presentation.home.SortType
import com.suseoaa.projectoaa.presentation.home.ValueCalculatorViewModel
import com.suseoaa.projectoaa.shared.database.ValueCalculatorItem
import com.suseoaa.projectoaa.ui.component.common.SharedTransitionPageContainer
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ValueCalculatorScreen(
    onBack: () -> Unit,
    viewModel: ValueCalculatorViewModel = koinViewModel()
) {
    val items by viewModel.items.collectAsState()
    val currentSort by viewModel.sortType.collectAsState()

    var itemName by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    
    // 日期选择相关状态
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    val datePickerState = rememberDatePickerState()

    // 监听选中时间的变化
    LaunchedEffect(datePickerState.selectedDateMillis) {
        selectedDateMillis = datePickerState.selectedDateMillis
    }

    val price = priceText.toDoubleOrNull()
    
    val selectedLocalDate = remember(selectedDateMillis) {
        selectedDateMillis?.let { millis ->
            Instant.fromEpochMilliseconds(millis)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
        }
    }
    
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    
    val daysSincePurchase = remember(selectedLocalDate) {
        selectedLocalDate?.let { date ->
            if (date <= today) {
                date.daysUntil(today)
            } else {
                null
            }
        }
    }

    SharedTransitionPageContainer(transitionKey = "value_calculator_feature") {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("物品价值计算", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 说明文本
                Text(
                    text = "计算你的物品买得值不值",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Text(
                    text = "输入购买信息，计算分摊到每一天的花费。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 卡片：输入区域
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        OutlinedTextField(
                            value = itemName,
                            onValueChange = { itemName = it },
                            label = { Text("物品名称") },
                            leadingIcon = {
                                Icon(Icons.Default.ShoppingBag, contentDescription = null)
                            },
                            colors = getCustomTextFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it },
                            label = { Text("购买价格 (¥)") },
                            leadingIcon = {
                                Icon(Icons.Default.Calculate, contentDescription = null)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = getCustomTextFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        val dateText = selectedLocalDate?.toString() ?: "未选择"
                        OutlinedTextField(
                            value = dateText,
                            onValueChange = { },
                            label = { Text("购买日期") },
                            leadingIcon = {
                                Icon(Icons.Default.DateRange, contentDescription = null)
                            },
                            enabled = false,
                            colors = getCustomTextFieldColors().copy(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledLabelColor = MaterialTheme.colorScheme.primary,
                                disabledLeadingIconColor = MaterialTheme.colorScheme.primary,
                                disabledIndicatorColor = MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { showDatePicker = true }
                                )
                        )
                    }
                }

                // 动画展示计算结果
                AnimatedVisibility(
                    visible = itemName.isNotBlank() && price != null && daysSincePurchase != null,
                    enter = fadeIn(tween(400)) + expandVertically(tween(400)),
                    exit = fadeOut(tween(400)) + shrinkVertically(tween(400))
                ) {
                    if (price != null && daysSincePurchase != null && selectedDateMillis != null) {
                        val actualDays = if (daysSincePurchase == 0) 1 else daysSincePurchase
                        val costPerDay = price / actualDays

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "结果速览",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                
                                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "已拥有天数",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = "$daysSincePurchase 天",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "日均花费",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = "¥",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(bottom = 4.dp, end = 2.dp)
                                        )
                                        val formattedCost = (costPerDay * 100).roundToInt() / 100.0
                                        Text(
                                            text = formattedCost.toString(),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 28.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                
                                // 趣味评价
                                val comment = when {
                                    costPerDay > 100 -> "有点小贵，努力让它发挥价值吧！"
                                    costPerDay > 30 -> "每天一杯咖啡钱，很合理！"
                                    costPerDay > 5 -> "性价比很高了，买得值！"
                                    else -> "这简直是白嫖，血赚！"
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = comment,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                }

                                // 加入资产按钮
                                Button(
                                    onClick = {
                                        viewModel.saveItem(itemName, price, selectedDateMillis!!)
                                        itemName = ""
                                        priceText = ""
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text("加入资产", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // 资产列表
                if (items.isNotEmpty()) {
                    val totalValue = items.sumOf { it.price }
                    val formattedTotal = (totalValue * 100).roundToInt() / 100.0
                    
                    val totalDailyCost = items.sumOf { item ->
                        val purchaseDate = Instant.fromEpochMilliseconds(item.purchaseDateMillis).toLocalDateTime(TimeZone.currentSystemDefault()).date
                        val daysSincePurchase = if (purchaseDate <= today) purchaseDate.daysUntil(today) else 0
                        val actualDays = if (daysSincePurchase == 0) 1 else daysSincePurchase
                        item.price / actualDays
                    }
                    val formattedTotalDaily = (totalDailyCost * 100).roundToInt() / 100.0
                    val itemCount = items.size
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "资产",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            
                            FadingSnappingSlider(
                                options = SortType.values().toList(),
                                currentSort = currentSort,
                                onSortChanged = { viewModel.updateSortType(it) },
                                modifier = Modifier.fillMaxWidth(0.5f)
                            )
                        }

                        // 常驻统计卡片
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("总数", color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f), fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("$itemCount 件", color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("总价值", color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f), fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("¥$formattedTotal", color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("总日均", color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f), fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("¥$formattedTotalDaily", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                }
                            }
                        }

                        items.forEach { item ->
                            HistoryItemCard(item = item, onDelete = { viewModel.deleteItem(item.id) })
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        // 确认选择已由 datePickerState 保存
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("确定", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text("取消")
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface,
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    headlineContentColor = MaterialTheme.colorScheme.onSurface,
                    weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    dayContentColor = MaterialTheme.colorScheme.onSurface,
                    disabledDayContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                    selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                    todayContentColor = MaterialTheme.colorScheme.primary,
                    todayDateBorderColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FadingSnappingSlider(
    options: List<SortType>,
    currentSort: SortType,
    onSortChanged: (SortType) -> Unit,
    modifier: Modifier = Modifier
) {
    val initialIndex = options.indexOf(currentSort).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    
    // 实时计算当前居中的选项索引，用于UI反馈（如字体颜色加粗等）
    val centerItemIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = layoutInfo.viewportStartOffset + layoutInfo.viewportSize.width / 2
            layoutInfo.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - viewportCenter) }?.index ?: initialIndex
        }
    }

    // 防抖与查询：当滑动完全停止后，才将最新的居中选项通知出去
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val finalSort = options.getOrNull(centerItemIndex)
            if (finalSort != null && finalSort != currentSort) {
                onSortChanged(finalSort)
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp),
        contentAlignment = Alignment.Center
    ) {
        // 近似计算单个选项的宽度，用于填充两侧的 padding，使首尾选项能停在正中心
        val approxItemWidth = 60.dp
        val horizontalPad = (maxWidth / 2) - (approxItemWidth / 2)

        LazyRow(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
            contentPadding = PaddingValues(horizontal = horizontalPad.coerceAtLeast(0.dp)),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.horizontalGradient(
                            0f to Color.Transparent,
                            0.2f to Color.Black,
                            0.8f to Color.Black,
                            1f to Color.Transparent
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
        ) {
            items(options.size) { index ->
                val sortType = options[index]
                val isSelected = (index == centerItemIndex)
                Text(
                    text = sortType.label,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = if (isSelected) 14.sp else 12.sp,
                    modifier = Modifier.clickable {
                        // 点击也可以直接切换并滚动到该项，但这属于额外体验
                    }
                )
            }
        }
    }
}

@Composable
fun HistoryItemCard(item: ValueCalculatorItem, onDelete: () -> Unit) {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val purchaseDate = Instant.fromEpochMilliseconds(item.purchaseDateMillis).toLocalDateTime(TimeZone.currentSystemDefault()).date
    
    val daysSincePurchase = if (purchaseDate <= today) purchaseDate.daysUntil(today) else 0
    val actualDays = if (daysSincePurchase == 0) 1 else daysSincePurchase
    val costPerDay = item.price / actualDays
    val formattedCost = (costPerDay * 100).roundToInt() / 100.0

    val offsetX = remember { Animatable(0f) }
    val maxSwipePx = with(LocalDensity.current) { -80.dp.toPx() }
    val coroutineScope = rememberCoroutineScope()
    var isDraggingAllowed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
    ) {
        // 删除按钮底层
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.error)
                .clickable { onDelete() }
                .padding(end = 24.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Icon(
                Icons.Default.Delete, 
                contentDescription = "删除", 
                tint = MaterialTheme.colorScheme.onError
            )
        }

        // 上层资产卡片
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            // 仅卡片右侧 1/3 区域允许触发向左滑动
                            isDraggingAllowed = offset.x > size.width * (2f / 3f)
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                // 如果向左滑动超过一半，则锁定在展开状态，否则回弹
                                if (offsetX.value < maxSwipePx / 2) {
                                    offsetX.animateTo(maxSwipePx)
                                } else {
                                    offsetX.animateTo(0f)
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch { offsetX.animateTo(0f) }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            if (isDraggingAllowed) {
                                change.consume()
                                coroutineScope.launch {
                                    // 限制只能往左滑（负值），最多滑动 maxSwipePx，往右最多回到 0
                                    val newOffset = (offsetX.value + dragAmount).coerceIn(maxSwipePx, 0f)
                                    offsetX.snapTo(newOffset)
                                }
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    // 如果处于展开状态，点击卡片其他位置则收回
                    detectTapGestures(
                        onTap = {
                            if (offsetX.value < 0) {
                                coroutineScope.launch { offsetX.animateTo(0f) }
                            }
                        }
                    )
                }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.itemName, 
                        fontWeight = FontWeight.Bold, 
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "购买于 $purchaseDate · ¥${item.price}", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "¥$formattedCost/天", 
                        fontWeight = FontWeight.ExtraBold, 
                        color = MaterialTheme.colorScheme.primary, 
                        fontSize = 16.sp
                    )
                    Text(
                        text = "$actualDays 天", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun getCustomTextFieldColors(): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
        unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
