package com.suseoaa.projectoaa.ui.screen.recruitment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.suseoaa.projectoaa.ui.animation.sharedBoundsTransition
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.suseoaa.projectoaa.ui.component.common.AdaptivePageScaffold
import com.suseoaa.projectoaa.ui.theme.AppDimensions
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun RecruitmentScreenPreview() {
    RecruitmentScreen(onBack = {})
}

@Composable
fun RecruitmentScreen(
    onBack: () -> Unit
) {
    AdaptivePageScaffold(
        modifier = Modifier.sharedBoundsTransition("recruitment_feature"),
        title = "招新换届",
        onBack = onBack,
        compactContent = { modifier ->
            RecruitmentCompactLayout(modifier = modifier)
        },
        tabletContent = { modifier ->
            RecruitmentTabletLayout(modifier = modifier)
        }
    )
}

@Composable
private fun RecruitmentCompactLayout(modifier: Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RecruitmentContentBlock(
            title = "这里是招新换届的具体内容",
            subtitle = "你可以在这里编写表单、介绍或者WebView。"
        )
    }
}

@Composable
private fun RecruitmentTabletLayout(modifier: Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.paneSpacing)
    ) {
        RecruitmentInfoPane(
            title = "招新换届介绍",
            subtitle = "这里放招新流程、活动说明和时间安排。",
            modifier = Modifier.weight(1f)
        )
        RecruitmentInfoPane(
            title = "报名与操作",
            subtitle = "这里放报名表单、联系方式和操作入口。",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RecruitmentInfoPane(
    title: String,
    subtitle: String,
    modifier: Modifier
) {
    Card(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(AppDimensions.cardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            RecruitmentContentBlock(title = title, subtitle = subtitle)
        }
    }
}

@Composable
private fun RecruitmentContentBlock(
    title: String,
    subtitle: String
) {
    Text(text = title, style = MaterialTheme.typography.headlineMedium)
    Spacer(modifier = Modifier.height(16.dp))
    Text(text = subtitle)
}