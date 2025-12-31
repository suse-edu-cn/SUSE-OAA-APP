package com.suseoaa.projectoaa.feature.changePassword

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

const val CHANGE_PASSWORD_ROUTE = "change_password_route"

fun NavController.navigateToChangePassword(navOptions: NavOptions? = null) {
    this.navigate(CHANGE_PASSWORD_ROUTE, navOptions)
}

//注册登录页
fun NavGraphBuilder.changePasswordScreen(
    onChangePasswordSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    composable(route = CHANGE_PASSWORD_ROUTE) {
        ChangePasswordScreen(
            onChangePasswordSuccess = onChangePasswordSuccess,
            onNavigateToLogin = onNavigateToLogin
        )
    }
}