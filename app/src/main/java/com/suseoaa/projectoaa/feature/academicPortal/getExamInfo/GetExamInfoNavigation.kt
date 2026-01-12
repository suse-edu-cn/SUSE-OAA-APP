package com.suseoaa.projectoaa.feature.academicPortal.getExamInfo

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

const val EXAM_ROUTE = "exam_route"

fun NavController.navigateToExam(navOptions: NavOptions? = null) {
    this.navigate(EXAM_ROUTE, navOptions)
}

fun NavGraphBuilder.examScreen(
    windowSizeClass: WindowWidthSizeClass,
    sharedTransitionScope: SharedTransitionScope
) {
    composable(route = EXAM_ROUTE) {
        GetExamInfoScreen(
            windowSizeClass = windowSizeClass,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = this
        )
    }
}