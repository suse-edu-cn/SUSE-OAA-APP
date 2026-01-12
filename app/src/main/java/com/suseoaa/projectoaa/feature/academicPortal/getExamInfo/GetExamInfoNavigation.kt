package com.suseoaa.projectoaa.feature.academicPortal.getExamInfo

import androidx.compose.animation.SharedTransitionScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

const val EXAM_ROUTE = "exam_route"

fun NavController.navigateToExam(navOptions: NavOptions? = null) {
    this.navigate(EXAM_ROUTE, navOptions)
}

fun NavGraphBuilder.examScreen(
    sharedTransitionScope: SharedTransitionScope
) {
    composable(route = EXAM_ROUTE) {
        GetExamInfoScreen(
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = this
        )
    }
}