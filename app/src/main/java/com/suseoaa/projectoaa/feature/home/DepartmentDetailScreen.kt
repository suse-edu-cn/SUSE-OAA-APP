package com.suseoaa.projectoaa.feature.home

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suseoaa.projectoaa.core.designsystem.theme.*
import com.suseoaa.projectoaa.core.ui.component.OaaMarkdownText
import com.suseoaa.projectoaa.core.util.AcademicSharedTransitionSpec

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DepartmentDetailScreen(
    departmentName: String,
    onBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    isTablet: Boolean = false,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(departmentName) {
        viewModel.fetchDetailInfo(departmentName)
    }

    // 全屏编辑弹窗
    if (uiState.showEditDialog) {
        Dialog(
            onDismissRequest = { viewModel.toggleEditDialog(false) },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Scaffold(
                containerColor = OxygenWhite,
                topBar = {
                    TopAppBar(
                        title = { Text("编辑${departmentName}介绍") },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.toggleEditDialog(false) }) {
                                Icon(Icons.Default.Close, "Close")
                            }
                        },
                        actions = {
                            TextButton(
                                onClick = { viewModel.submitUpdate() },
                                enabled = !uiState.isUpdating
                            ) {
                                Text("保存", fontWeight = FontWeight.Bold)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = OxygenWhite)
                    )
                }
            ) { padding ->
                Box(modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()) {
                    if (uiState.isUpdating) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    TextField(
                        value = uiState.editContent,
                        onValueChange = { viewModel.onEditContentChange(it) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                        ),
                        placeholder = { Text("在此输入 Markdown 内容...") }
                    )
                }
            }
        }
    }

    // 主内容
    with(sharedTransitionScope) {
        val modifier = if (isTablet) Modifier.fillMaxSize() else Modifier
            .fillMaxSize()
            .sharedBounds(
                sharedContentState = rememberSharedContentState(key = "card_$departmentName"),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = AcademicSharedTransitionSpec,
                resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
            )

        Surface(
            modifier = modifier,
            color = OxygenWhite
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    if (!isTablet) {
                        CenterAlignedTopAppBar(
                            title = { Text(departmentName, fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = onBack) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = OxygenWhite
                            ),
                            modifier = Modifier.statusBarsPadding()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Text(
                                text = departmentName,
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = InkBlack
                            )
                        }
                    }
                },
                floatingActionButton = {
                    AnimatedVisibility(
                        visible = uiState.canEditCurrent && uiState.detailData != null,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        FloatingActionButton(
                            onClick = { viewModel.toggleEditDialog(true) },
                            containerColor = ElectricBlue,
                            contentColor = OxygenWhite
                        ) {
                            Icon(Icons.Default.Edit, "Edit")
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    when {
                        uiState.isLoadingDetail && !uiState.isUpdating -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = ElectricBlue
                            )
                        }

                        uiState.detailError != null -> {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    null,
                                    tint = AlertRed,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text("加载失败", style = MaterialTheme.typography.titleMedium)
                                Text(uiState.detailError ?: "", color = InkGrey)
                                Button(onClick = { viewModel.fetchDetailInfo(departmentName) }) {
                                    Text("重试")
                                }
                            }
                        }

                        uiState.detailData != null -> {
                            LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                                item {
                                    if (!isTablet) Spacer(modifier = Modifier.height(16.dp))
                                    OaaMarkdownText(
                                        markdown = uiState.detailData!!.data,
                                        modifier = Modifier.fillMaxWidth(),
                                        color = InkBlack,
                                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}