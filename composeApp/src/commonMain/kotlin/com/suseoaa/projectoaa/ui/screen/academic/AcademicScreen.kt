package com.suseoaa.projectoaa.ui.screen.academic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suseoaa.projectoaa.presentation.academic.AcademicViewModel
import com.suseoaa.projectoaa.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

data class PortalFunction(
    val title: String,
    val icon: ImageVector,
    val route: String,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicScreen(
    onNavigateToGrades: () -> Unit,
    onNavigateToGpa: () -> Unit,
    onNavigateToExams: () -> Unit,
    viewModel: AcademicViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomContentPadding = 80.dp + navBarHeight

    val functions = listOf(
        PortalFunction(
            "成绩查询",
            Icons.Default.List,
            "grades",
            ElectricBlue
        ),
        PortalFunction(
            "绩点计算",
            Icons.Default.Star,
            "gpa",
            Color(0xFF26A69A)
        )
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(
            top = 16.dp + statusBarHeight,
            bottom = 16.dp + bottomContentPadding,
            start = 16.dp,
            end = 16.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. 调课信息卡片
        item(span = { GridItemSpan(maxLineSpan) }) {
            ReschedulingCard(
                messageList = uiState.messages,
                onClick = { /* TODO: 导航到消息详情 */ }
            )
        }

        // 2. 考试信息卡片
        item(span = { GridItemSpan(maxLineSpan) }) {
            UpcomingExamsCard(
                examList = uiState.exams,
                onClick = onNavigateToExams
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = "常用功能",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        items(functions) { func ->
            FunctionCard(
                func = func,
                onClick = {
                    when (func.route) {
                        "grades" -> onNavigateToGrades()
                        "gpa" -> onNavigateToGpa()
                    }
                }
            )
        }
    }
}

@Composable
fun ReschedulingCard(
    messageList: List<String>,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OxygenWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint = ElectricBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "调课通知",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (messageList.isEmpty()) {
                Text(
                    text = "暂无调课通知",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkGrey
                )
            } else {
                messageList.take(3).forEach { message ->
                    Text(
                        text = "• $message",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkGrey,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun UpcomingExamsCard(
    examList: List<com.suseoaa.projectoaa.shared.domain.model.exam.ExamItem>,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OxygenWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    tint = Color(0xFFFF6B6B),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "近期考试",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (examList.isEmpty()) {
                Text(
                    text = "暂无近期考试",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkGrey
                )
            } else {
                examList.take(3).forEach { exam ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = exam.kcmc ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkBlack,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = exam.kssj ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkGrey
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun FunctionCard(
    func: PortalFunction,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OxygenWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                func.icon,
                contentDescription = null,
                tint = func.color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = func.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
