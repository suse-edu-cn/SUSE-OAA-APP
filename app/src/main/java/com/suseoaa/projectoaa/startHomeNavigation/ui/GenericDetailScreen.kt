package com.suseoaa.projectoaa.navigation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment // 1. 导入
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suseoaa.projectoaa.navigation.viewmodel.DetailBlock
import com.suseoaa.projectoaa.navigation.viewmodel.GenericDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericDetailScreen(
    onBack: () -> Unit,
    viewModel: GenericDetailViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(uiState.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f),
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->

        // [修改] 2. 添加 Box 以便居中显示加载动画
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                // 3. 正在加载时，显示居中的加载动画
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                // 4. 加载完成后，显示列表
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp), // 5. 使用 contentPadding
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.blocks) { block ->
                        InfoBlockCard(block = block)
                    }
                    item { Spacer(modifier = Modifier.height(60.dp)) }
                }
            }
        }
    }
}

/**
 * “标准信息块”的 UI (可重用)
 * (此组件无需修改)
 */
@Composable
private fun InfoBlockCard(block: DetailBlock) {
    AppCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Text(
                text = block.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = block.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}