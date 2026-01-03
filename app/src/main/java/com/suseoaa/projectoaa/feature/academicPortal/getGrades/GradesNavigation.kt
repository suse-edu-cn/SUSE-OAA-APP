package com.suseoaa.projectoaa.feature.academicPortal.getGrades

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

const val GRADES_ROUTE = "grades_route"

fun NavController.navigateToGrades(navOptions: NavOptions? = null) {
    this.navigate(GRADES_ROUTE, navOptions)
}

fun NavGraphBuilder.gradesScreen() {
    composable(route = GRADES_ROUTE) {
        GradesScreen()
    }
}