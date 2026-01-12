package com.suseoaa.projectoaa.feature.academicPortal

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.feature.academicPortal.getGrades.GradesScreen
import com.suseoaa.projectoaa.feature.testScreen.ScreenState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suseoaa.projectoaa.core.ui.BaseInfoViewModel
import com.suseoaa.projectoaa.core.util.AcademicSharedTransitionSpec
import com.suseoaa.projectoaa.feature.academicPortal.getMessageInfo.GetAcademicMessageInfoViewModel
import kotlinx.coroutines.Dispatchers

@Composable
fun AcademicScreen(
    isTablet: Boolean,
    onNavigate: (AcademicPortalEvent) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    // 1. 在父层获取 ViewModel
    val messageVM: GetAcademicMessageInfoViewModel = hiltViewModel()
    // 2. 收集数据状态
    val messageList by messageVM.dataList.collectAsStateWithLifecycle()


    with(sharedTransitionScope) {
        // 4. 使用 Column 确保垂直排列
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp) // 组件间距
        ) {

            // --- 信息列表卡片 ---
            // 这里把拿到的 list 传进去
            InfoCards(
                title = "调课信息",
                infoList = messageList,
                viewModel = messageVM
            )
            // --- 成绩查询卡片 ---
            Row(
                modifier = Modifier
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "grades_card_key"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = AcademicSharedTransitionSpec,
                        resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
                    )
                    .size(120.dp, 80.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        onNavigate(AcademicPortalEvent.NavigateTo(AcademicDestinations.Grades))
                    }
            ) {
                Text(
                    text = "成绩查询",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }


        }
    }
}

@Composable
fun InfoCards(
    title: String,
    viewModel: BaseInfoViewModel<List<String>>,
    infoList: List<String>?, // 允许为空
    modifier: Modifier = Modifier // 允许外部控制大小位置
) {
    LaunchedEffect(Unit) {
        viewModel.fetchData()
    }
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 标题区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            // 列表区域
            if (infoList.isNullOrEmpty()) {
                // 处理空状态
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无数据", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f) // 关键：占据剩余高度
                ) {
                    items(infoList) { item ->
                        Text(
                            text = item,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}

//@Preview
//@Composable
//fun Preview() {
//    val list = listOf("aaaaaaa", "bbbbbbbbb", "ccccccccccc")
//    InfoCards("调课通知", list)
//}