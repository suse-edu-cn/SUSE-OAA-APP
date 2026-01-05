package com.suseoaa.projectoaa.feature.academicPortal

import androidx.compose.animation.SharedTransitionScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

const val ACADEMIC_ROUTE = "academic_route"

fun NavController.navigateToAcademic(navOptions: NavOptions? = null) {
    this.navigate(ACADEMIC_ROUTE, navOptions)
}

fun NavGraphBuilder.academicScreen(
    onNavigate: (AcademicPortalEvent) -> Unit,
    sharedTransitionScope: SharedTransitionScope
) {
    composable(route = ACADEMIC_ROUTE) {
        AcademicScreen(
            isTablet = false,
            onNavigate = onNavigate,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = this
        )
    }
}