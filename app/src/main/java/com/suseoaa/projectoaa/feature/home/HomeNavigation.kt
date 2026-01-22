package com.suseoaa.projectoaa.feature.home

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

const val HOME_ROUTE = "home_route"
const val DEPARTMENT_DETAIL_ROUTE = "department_detail_route"
const val DEPT_NAME_ARG = "deptName"

fun NavController.navigateToHome(navOptions: NavOptions? = null) {
    this.navigate(HOME_ROUTE, navOptions)
}

fun NavController.navigateToDepartmentDetail(deptName: String) {
    this.navigate("$DEPARTMENT_DETAIL_ROUTE/$deptName")
}

@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.homeScreen(
    isTablet: Boolean,
    sharedTransitionScope: SharedTransitionScope,
    onNavigateToDetail: (String) -> Unit
) {
    composable(route = HOME_ROUTE) {
        HomeScreen(
            isTablet = isTablet,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = this,
            onNavigateToDetail = onNavigateToDetail
        )
    }
}