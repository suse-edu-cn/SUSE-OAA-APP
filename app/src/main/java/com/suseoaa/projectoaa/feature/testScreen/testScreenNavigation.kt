package com.suseoaa.projectoaa.feature.testScreen

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

const val TEST_ROUTE = "test_route"

fun NavController.navigateToTest(navOptions: NavOptions? = null) {
    this.navigate(TEST_ROUTE, navOptions)
}

fun NavGraphBuilder.testScreen() {
    composable(route = TEST_ROUTE) {
        TestScreen()
    }
}