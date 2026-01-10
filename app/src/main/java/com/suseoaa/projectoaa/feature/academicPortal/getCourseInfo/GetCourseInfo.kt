package com.suseoaa.projectoaa.feature.academicPortal.getCourseInfo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suseoaa.projectoaa.feature.home.MessageInfo
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

@Composable
fun GetCourseInfo(viewModel: GetCourseInfoViewModel = hiltViewModel()) {
    // 1. 收集数据
    val infoList by viewModel.courseInfoList.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    // 2. 自动触发请求 (进入页面时)
    LaunchedEffect(Unit) {
        viewModel.fetchAcademicCourseInfo()
    }

    // 3. 界面布局
    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp) // 列表项之间的间距
    ) {
        if (isLoading) {
            item { Text("正在加载...") }
        } else if (infoList.isEmpty()) {
            item { Text("暂无通知消息") }
        } else {
            items(infoList) { info ->
                // 使用 Card 包裹让显示更好看
                Card(
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = info,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}