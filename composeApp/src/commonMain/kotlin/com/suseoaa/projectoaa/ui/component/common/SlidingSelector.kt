package com.suseoaa.projectoaa.ui.component.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun SlidingSelector(
    options: List<String>,
    selectedIndices: Set<Int>,
    onSelectionChanged: (Set<Int>) -> Unit,
    modifier: Modifier = Modifier,
    textFontSize: TextUnit = 14.sp
) {
    if (options.isEmpty()) return

    val isDarkTheme = isSystemInDarkTheme()
    val trackColor = if (isDarkTheme) Color(0xFF1C1C1E).copy(alpha = 0.8f) else Color(0xFFF2F4F6)
    val thumbColor = if (isDarkTheme) Color(0xFF3A3A3C) else Color.White
    val selectedTextColor = if (isDarkTheme) Color.White else Color(0xFF191C1E)
    val unselectedTextColor = if (isDarkTheme) Color(0xFF8E8E93) else Color(0xFF70767E)

    var containerWidth by remember { mutableStateOf(0f) }
    
    val density = LocalDensity.current
    val paddingPx = remember(density) { with(density) { 4.dp.toPx() } }
    val innerContainerWidth = maxOf(0f, containerWidth - paddingPx * 2)
    val optionWidth = if (options.isNotEmpty() && innerContainerWidth > 0) innerContainerWidth / options.size else 0f

    // Helper to get contiguous ranges
    val contiguousRanges = remember(selectedIndices) {
        if (selectedIndices.isEmpty()) return@remember emptyList<IntRange>()
        val sorted = selectedIndices.sorted()
        val ranges = mutableListOf<IntRange>()
        var start = sorted.first()
        var end = start

        for (i in 1 until sorted.size) {
            val current = sorted[i]
            if (current == end + 1) {
                end = current
            } else {
                ranges.add(start..end)
                start = current
                end = current
            }
        }
        ranges.add(start..end)
        ranges
    }

    val currentSelectedIndices by rememberUpdatedState(selectedIndices)
    val currentOnSelectionChanged by rememberUpdatedState(onSelectionChanged)

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(trackColor)
            .onGloballyPositioned { coordinates ->
                containerWidth = coordinates.size.width.toFloat()
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        if (optionWidth > 0) {
                            val adjustedX = offset.x - paddingPx
                            val clickedIndex = (adjustedX / optionWidth).toInt().coerceIn(0, options.lastIndex)
                            val newSelection = currentSelectedIndices.toMutableSet()
                            if (newSelection.contains(clickedIndex)) {
                                newSelection.remove(clickedIndex)
                            } else {
                                newSelection.add(clickedIndex)
                            }
                            currentOnSelectionChanged(newSelection)
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                var initialSelection = setOf<Int>()
                var dragStartIndex = -1
                var lastDragIndex = -1
                var minVisitedIndex = -1
                var maxVisitedIndex = -1
                var paintMode: Boolean? = null
                
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        if (optionWidth <= 0) return@detectHorizontalDragGestures
                        val adjustedX = offset.x - paddingPx
                        dragStartIndex = (adjustedX / optionWidth).toInt().coerceIn(0, options.lastIndex)
                        lastDragIndex = dragStartIndex
                        minVisitedIndex = dragStartIndex
                        maxVisitedIndex = dragStartIndex
                        initialSelection = currentSelectedIndices.toSet()
                        
                        if (!initialSelection.contains(dragStartIndex)) {
                            paintMode = true
                            val newSelection = initialSelection.toMutableSet()
                            newSelection.add(dragStartIndex)
                            currentOnSelectionChanged(newSelection)
                        } else {
                            paintMode = null
                        }
                    },
                    onDragEnd = {
                        dragStartIndex = -1
                        lastDragIndex = -1
                        minVisitedIndex = -1
                        maxVisitedIndex = -1
                        paintMode = null
                        initialSelection = emptySet()
                    },
                    onDragCancel = {
                        dragStartIndex = -1
                        lastDragIndex = -1
                        minVisitedIndex = -1
                        maxVisitedIndex = -1
                        paintMode = null
                        initialSelection = emptySet()
                    },
                    onHorizontalDrag = { change, _ ->
                        if (dragStartIndex != -1 && optionWidth > 0) {
                            val adjustedX = change.position.x - paddingPx
                            val currentIndex = (adjustedX / optionWidth).toInt().coerceIn(0, options.lastIndex)
                            if (currentIndex != lastDragIndex) {
                                if (paintMode == null) {
                                    paintMode = !initialSelection.contains(currentIndex)
                                }
                                
                                minVisitedIndex = minOf(minVisitedIndex, currentIndex)
                                maxVisitedIndex = maxOf(maxVisitedIndex, currentIndex)
                                
                                val newSelection = initialSelection.toMutableSet()
                                // Erase everything in the maximum visited range of THIS drag
                                for (i in minVisitedIndex..maxVisitedIndex) {
                                    if (paintMode == true) newSelection.remove(i) else newSelection.add(i)
                                }
                                
                                // Then, apply the current active drag range
                                val range = if (dragStartIndex < currentIndex) dragStartIndex..currentIndex else currentIndex..dragStartIndex
                                for (i in range) {
                                    if (paintMode == true) {
                                        newSelection.add(i)
                                    } else {
                                        // When deselecting, treat the current finger position as the new selection boundary
                                        // so it remains selected (exclude it from being removed).
                                        if (i != currentIndex) {
                                            newSelection.remove(i)
                                        }
                                    }
                                }
                                
                                lastDragIndex = currentIndex
                                currentOnSelectionChanged(newSelection)
                            }
                        }
                    }
                )
            }
            .padding(4.dp)
    ) {
        // Draw the contiguous background ranges
        if (optionWidth > 0f) {
            contiguousRanges.forEach { range ->
                val startOffset = range.first * optionWidth
                val width = (range.last - range.first + 1) * optionWidth
                
                Box(
                    modifier = Modifier
                        .offset(x = with(LocalDensity.current) { startOffset.toDp() })
                        .width(with(LocalDensity.current) { width.toDp() })
                        .fillMaxHeight()
                        .shadow(elevation = if (isDarkTheme) 0.dp else 2.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .background(thumbColor)
                )
            }
        }

        Row(
            modifier = Modifier.matchParentSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { index, text ->
                val isSelected = selectedIndices.contains(index)
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = text,
                        color = if (isSelected) selectedTextColor else unselectedTextColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = textFontSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
