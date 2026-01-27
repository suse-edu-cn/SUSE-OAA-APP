package com.suseoaa.projectoaa.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography

/**
 * Markdown 文本组件
 */
@Composable
fun OaaMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    // 定义代码块背景色
    val codeBgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    Markdown(
        content = markdown,
        modifier = modifier.fillMaxWidth(),
        // 文字样式配置
        typography = markdownTypography(
            h1 = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            h2 = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            h3 = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            paragraph = style,
            bullet = style,
            list = style,
            quote = style.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
        ),
        // 颜色配置
        colors = markdownColor(
            text = color,
            codeBackground = codeBgColor,
            inlineCodeBackground = codeBgColor
        )
    )
}
