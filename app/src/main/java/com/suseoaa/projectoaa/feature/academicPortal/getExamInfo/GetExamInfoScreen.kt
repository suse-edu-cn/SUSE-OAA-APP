package com.suseoaa.projectoaa.feature.academicPortal.getExamInfo

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun GetExamInfoScreen(
    viewModel: GetExamInfoViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {

}