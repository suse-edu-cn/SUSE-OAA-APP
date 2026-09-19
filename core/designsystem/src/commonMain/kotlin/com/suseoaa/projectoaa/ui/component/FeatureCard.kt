package com.suseoaa.projectoaa.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.ui.animation.sharedBoundsTransition

// 功能入口卡片。首页与教务门户共用，因此放在设计系统而不是某一个功能模块里。

// 样式 3: 独立功能卡片 (不同于介绍卡片)
@Composable
fun FeatureCard(
    name: String,
    icon: ImageVector,
    color: Color,
    onColor: Color,
    onClick: () -> Unit,
    sharedBoundKey: String
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .sharedBoundsTransition(sharedBoundKey),
        shape = RoundedCornerShape(16.dp),
        color = color,
        border = BorderStroke(1.dp, onColor.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                tint = onColor,
                modifier = Modifier.size(32.dp).padding(bottom = 8.dp)
            )
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = onColor
            )
        }
    }
}
