package com.suseoaa.projectoaa.feature.academicPortal.getCourseInfo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.suseoaa.projectoaa.feature.academicPortal.getExamInfo.GetExamInfoViewModel
import com.suseoaa.projectoaa.feature.academicPortal.getMessageInfo.GetAcademicMessageInfoViewModel

@Composable
fun GetCourseInfo() {
    Column(modifier = Modifier.fillMaxSize()) {
        // 上半部分：AreaOne (通知)
        val areaOneVM: GetCourseInfoViewModel = hiltViewModel()

        Text(
            "教务通知",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.primary
        )

        CommonInfoListScreen(
            viewModel = areaOneVM,
            modifier = Modifier.weight(1f) // [关键] 分配 50% 高度
        )

        HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.surfaceVariant)

        // 下半部分：AreaThree (消息/AreaFour)
        val areaThreeVM: GetAcademicMessageInfoViewModel = hiltViewModel()

        Text(
            "调课信息",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.primary
        )

        CommonInfoListScreen(
            viewModel = areaThreeVM,
            modifier = Modifier.weight(1f) // [关键] 分配 50% 高度
        )

        HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.surfaceVariant)


        val areaFourVM: GetExamInfoViewModel = hiltViewModel()
        Text(
            "考试信息",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.primary
        )

        CommonInfoListScreen(
            viewModel = areaFourVM,
            modifier = Modifier.weight(1f) // [关键] 分配 50% 高度
        )
    }
}

@Composable
fun CommonInfoListScreen(
    viewModel: BaseInfoViewModel<List<String>>,
    modifier: Modifier = Modifier // [关键] 接收外部传入的 Modifier
) {
    val list by viewModel.dataList.collectAsStateWithLifecycle()
    val loading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.errorMessage.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.fetchData()
    }

    // 使用 Box 包裹，方便处理居中 loading 或空状态
    Box(modifier = modifier.fillMaxSize()) {
        when {
            loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            error != null -> {
                Text(
                    text = "错误: $error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            list.isNullOrEmpty() -> {
                // [关键] 空状态提示
                Text(
                    text = "暂无相关消息",
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(list!!) { item ->
                        Text(
                            text = item,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}