package com.suseoaa.projectoaa.feature.person

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.suseoaa.projectoaa.app.LocalWindowSizeClass
import com.suseoaa.projectoaa.core.network.model.person.Data
import kotlinx.serialization.SerialName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonScreen(
    viewModel: PersonViewModel = hiltViewModel()
) {
    val userInfo by viewModel.userInfo.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.errorMessage.collectAsStateWithLifecycle()
    val windowsSizeClass = LocalWindowSizeClass.current
    val isPhone = windowsSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
    val column = if (isPhone) 2 else 4
    Scaffold() { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (error != null) {
                // 错误提示及重试按钮
                Column(modifier = Modifier.align(Alignment.Center)) {
                    Text(text = "加载失败: $error", color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.fetchPersonInfo() }) {
                        Text("重试")
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(column),
                    //        设置内边距
                    contentPadding = PaddingValues(16.dp),
                    //        设置子项之间的间距
                    // 水平间距
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    // 垂直间距
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text("fhuihfi")
                    }
                }
            }
        }
    }
}


@Preview
@Composable
fun Preview() {
    val TestData = Data(
        avatar = "https://fuck-suse-img-bucket.oss-cn-chengdu.aliyuncs.com/images%2Fdefault.jpg?Expires=1768467390&OSSAccessKeyId=LTAI5tGETXk3Zc8gaMEFGFL7&Signature=N3mMtincId%2FxA%2BFd6qcgK5HIr8w%3D",
        department = "项目部",
        name = "Vincent",
        role = "会员",
        studentId = "23341010304",
        username = "Vincent"
    )
    PersonInfo(userInfo = TestData)
}

@Composable
fun PersonInfo(userInfo: Data) {
    InfoCards()

}

@Composable
fun InfoCards() {
    Text("fhgusklfhskf")
}