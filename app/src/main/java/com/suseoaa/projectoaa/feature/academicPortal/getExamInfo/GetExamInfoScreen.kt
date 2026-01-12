package com.suseoaa.projectoaa.feature.academicPortal.getExamInfo

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suseoaa.projectoaa.core.ui.BaseInfoViewModel
import com.suseoaa.projectoaa.core.util.AcademicSharedTransitionSpec
import com.suseoaa.projectoaa.feature.academicPortal.AcademicDestinations
import com.suseoaa.projectoaa.feature.academicPortal.AcademicPortalEvent
import com.suseoaa.projectoaa.feature.academicPortal.InfoCards
import com.suseoaa.projectoaa.feature.academicPortal.getGrades.GradeItemCard
import com.suseoaa.projectoaa.feature.academicPortal.getGrades.SelectOption
import com.suseoaa.projectoaa.feature.academicPortal.getMessageInfo.GetAcademicMessageInfoViewModel

@Composable
fun GetExamInfoScreen(
    viewModel: GetExamInfoViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    // 1. 在父层获取 ViewModel
    val messageVM: GetExamInfoViewModel = hiltViewModel()
    // 2. 收集数据状态
    val messageList by messageVM.dataList.collectAsStateWithLifecycle()
    with(sharedTransitionScope) {
        Scaffold(
            modifier = Modifier
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "exam_card_key"),
                    animatedVisibilityScope = animatedVisibilityScope,
//                    使用复用的动画预设
                    boundsTransform = AcademicSharedTransitionSpec
                ),
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                ExamList(
                    title = "Test",
                    infoList = messageList,
                    viewModel = messageVM,
                    modifier = Modifier
                )
            }
        }
    }
}

@Composable
fun ExamList(
    title: String,
    infoList: List<String>?,
    viewModel: BaseInfoViewModel<List<String>>,
    modifier: Modifier
) {
    LaunchedEffect(Unit) {
        viewModel.fetchData()
    }


    Text(title)
    if (infoList.isNullOrEmpty()) {
        // 处理空状态
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无数据", color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier
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