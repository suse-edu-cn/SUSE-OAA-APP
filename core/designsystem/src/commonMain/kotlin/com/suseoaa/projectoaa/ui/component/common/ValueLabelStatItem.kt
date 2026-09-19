package com.suseoaa.projectoaa.ui.component.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Column

@Composable
fun ValueLabelStatItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    valueTextStyle: TextStyle = MaterialTheme.typography.titleLarge,
    labelTextStyle: TextStyle = MaterialTheme.typography.bodySmall,
    valueFontWeight: FontWeight = FontWeight.Bold,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = valueTextStyle,
            fontWeight = valueFontWeight,
            color = color
        )
        Text(
            text = label,
            style = labelTextStyle,
            color = labelColor
        )
    }
}

