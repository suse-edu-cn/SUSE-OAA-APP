package com.suseoaa.projectoaa.feature.home

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel


@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val announcementInfo by viewModel.announcementInfo.collectAsStateWithLifecycle()
    Text(announcementInfo?.data ?: "测试")
}