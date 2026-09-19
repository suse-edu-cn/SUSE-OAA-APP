package com.suseoaa.projectoaa.ui.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.isSystemInDarkTheme
import coil3.compose.AsyncImage
import com.suseoaa.projectoaa.shared.data.local.store.BackgroundPageIds
import com.suseoaa.projectoaa.ui.component.LocalMainTabVisible
import com.suseoaa.projectoaa.ui.screen.academic.AcademicScreen
import com.suseoaa.projectoaa.ui.screen.course.CourseScreen
import com.suseoaa.projectoaa.ui.screen.home.HomeScreen
import com.suseoaa.projectoaa.ui.screen.person.PersonScreen
import com.suseoaa.projectoaa.ui.theme.*
import com.suseoaa.projectoaa.util.decodeBackgroundImage

// 四个主 Tab 页的常驻承载与各页背景图。

@Composable
internal fun KeepAliveMainPages(
    selectedTab: Int,
    bottomBarHeight: Dp,
    homeFeatureDrawerExpanded: Boolean,
    onHomeFeatureDrawerExpandedChange: (Boolean) -> Unit,
    academicFeatureDrawerExpanded: Boolean,
    onAcademicFeatureDrawerExpandedChange: (Boolean) -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    onNavigateToGrades: () -> Unit,
    onNavigateToGpa: () -> Unit,
    onNavigateToExams: () -> Unit,
    onNavigateToAcademicMessages: () -> Unit,
    onNavigateToDepartmentDetail: (String) -> Unit,
    onNavigateToStudyRequirement: () -> Unit,
    onNavigateToCourseInfo: () -> Unit,
    onNavigateToAcademicStatus: () -> Unit,
    onNavigateToCheckin: () -> Unit,
    onNavigateToRecruitment: () -> Unit,
    onNavigateToUserQuery: () -> Unit,
    onNavigateToActivityCheckin: () -> Unit,
    onNavigateToValueCalculator: () -> Unit,
    onNavigateToUpdate: () -> Unit, onNavigateToSettings: () -> Unit,
    onNavigateToCourseStatistics: () -> Unit,
) {
    val orderedTabs = remember(selectedTab) {
        MainTab.entries.sortedBy { if (it.index == selectedTab) 1 else 0 }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        orderedTabs.forEach { tab ->
            // key(tab.index) 确保 Tab 顺序重排时 Compose 移动而非销毁重建 composable，
            // 避免抽屉状态（isInitialized、offsetYAnim 等）在每次切换时被重置
            key(tab.index) {
                val isVisible = tab.index == selectedTab

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            if (isVisible) drawContent()
                        }
                        .graphicsLayer {
                            alpha = if (isVisible) 1f else 0f
                        }
                ) {
                    MainTabPage(
                        tabIndex = tab.index,
                        isVisible = isVisible,
                        bottomBarHeight = bottomBarHeight,
                        homeFeatureDrawerExpanded = homeFeatureDrawerExpanded,
                        onHomeFeatureDrawerExpandedChange = onHomeFeatureDrawerExpandedChange,
                        academicFeatureDrawerExpanded = academicFeatureDrawerExpanded,
                        onAcademicFeatureDrawerExpandedChange = onAcademicFeatureDrawerExpandedChange,
                        onNavigateToLogin = onNavigateToLogin,
                        onNavigateToChangePassword = onNavigateToChangePassword,
                        onNavigateToGrades = onNavigateToGrades,
                        onNavigateToGpa = onNavigateToGpa,
                        onNavigateToExams = onNavigateToExams,
                        onNavigateToAcademicMessages = onNavigateToAcademicMessages,
                        onNavigateToDepartmentDetail = onNavigateToDepartmentDetail,
                        onNavigateToStudyRequirement = onNavigateToStudyRequirement,
                        onNavigateToCourseInfo = onNavigateToCourseInfo,
                        onNavigateToAcademicStatus = onNavigateToAcademicStatus,
                        onNavigateToCheckin = onNavigateToCheckin,
                        onNavigateToRecruitment = onNavigateToRecruitment,
                        onNavigateToUserQuery = onNavigateToUserQuery,
                        onNavigateToActivityCheckin = onNavigateToActivityCheckin,
                        onNavigateToValueCalculator = onNavigateToValueCalculator,
                        onNavigateToUpdate = onNavigateToUpdate, onNavigateToSettings = onNavigateToSettings,
                        onNavigateToCourseStatistics = onNavigateToCourseStatistics,
                    )
                }
            } // end key(tab.index)
        }
    }
}

@Composable
internal fun MainTabPage(
    tabIndex: Int,
    isVisible: Boolean,
    bottomBarHeight: Dp,
    homeFeatureDrawerExpanded: Boolean,
    onHomeFeatureDrawerExpandedChange: (Boolean) -> Unit,
    academicFeatureDrawerExpanded: Boolean,
    onAcademicFeatureDrawerExpandedChange: (Boolean) -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    onNavigateToGrades: () -> Unit,
    onNavigateToGpa: () -> Unit,
    onNavigateToExams: () -> Unit,
    onNavigateToAcademicMessages: () -> Unit,
    onNavigateToDepartmentDetail: (String) -> Unit,
    onNavigateToStudyRequirement: () -> Unit,
    onNavigateToCourseInfo: () -> Unit,
    onNavigateToAcademicStatus: () -> Unit,
    onNavigateToCheckin: () -> Unit,
    onNavigateToRecruitment: () -> Unit,
    onNavigateToUserQuery: () -> Unit,
    onNavigateToActivityCheckin: () -> Unit,
    onNavigateToValueCalculator: () -> Unit,
    onNavigateToUpdate: () -> Unit, onNavigateToSettings: () -> Unit,
    onNavigateToCourseStatistics: () -> Unit,
) {
    CompositionLocalProvider(LocalMainTabVisible provides isVisible) {
        when (tabIndex) {
            MainTab.HOME.index -> HomeScreen(
                onNavigateToDetail = onNavigateToDepartmentDetail,
                bottomBarHeight = bottomBarHeight,
                onNavigateToRecruitment = onNavigateToRecruitment,
                onNavigateToUserQuery = onNavigateToUserQuery,
                featureDrawerExpanded = homeFeatureDrawerExpanded,
                onFeatureDrawerExpandedChange = onHomeFeatureDrawerExpandedChange,
                onNavigateToActivityCheckin = onNavigateToActivityCheckin,
                onNavigateToValueCalculator = onNavigateToValueCalculator
            )

            MainTab.COURSE.index -> CourseScreen(
                onNavigateToLogin = onNavigateToLogin,
                onNavigateToCourseStatistics = onNavigateToCourseStatistics,
                bottomBarHeight = bottomBarHeight
            )

            MainTab.ACADEMIC.index -> AcademicScreen(
                onNavigateToGrades = onNavigateToGrades,
                onNavigateToGpa = onNavigateToGpa,
                onNavigateToExams = onNavigateToExams,
                onNavigateToRescheduling = onNavigateToAcademicMessages,
                onNavigateToStudyRequirement = onNavigateToStudyRequirement,
                onNavigateToCourseInfo = onNavigateToCourseInfo,
                onNavigateToAcademicStatus = onNavigateToAcademicStatus,
                featureDrawerExpanded = academicFeatureDrawerExpanded,
                onFeatureDrawerExpandedChange = onAcademicFeatureDrawerExpandedChange,
                bottomBarHeight = bottomBarHeight
            )

            MainTab.PERSON.index -> PersonScreen(
                onNavigateToLogin = onNavigateToLogin,
                onNavigateToChangePassword = onNavigateToChangePassword,
                onNavigateToCheckin = onNavigateToCheckin,
                onNavigateToUpdate = onNavigateToUpdate,
                onNavigateToSettings = onNavigateToSettings,
                bottomBarHeight = bottomBarHeight
            )
        }
    }
}

internal fun resolveBackgroundImage(
    appBackgroundImages: Map<String, String?>,
    tabIndex: Int
): String? {
    return if (tabIndex == MainTab.COURSE.index) {
        appBackgroundImages[BackgroundPageIds.COURSE]
    } else {
        null
    }
}

@Composable
internal fun MainPageBackground(
    encodedImage: String?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val imageBytes = remember(encodedImage) { decodeBackgroundImage(encodedImage) }
    val isDarkTheme = isSystemInDarkTheme()
    val scrimAlpha = if (isDarkTheme) 0.38f else 0.24f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (imageBytes != null) {
            AsyncImage(
                model = imageBytes,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = scrimAlpha))
            )
        }

        content()
    }
}
