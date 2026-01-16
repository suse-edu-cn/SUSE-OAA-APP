package com.suseoaa.projectoaa.feature.person

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

const val PERSON_ROUTE = "person_route"

fun NavController.navigateToPerson(navOptions: NavOptions? = null) {
    this.navigate(PERSON_ROUTE, navOptions)
}

fun NavGraphBuilder.personScreen(
    onNavigateToLogin: () -> Unit
) {
    composable(route = PERSON_ROUTE) {
        PersonScreen(
            onNavigateToLogin = onNavigateToLogin
        )
    }
}