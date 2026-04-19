package com.suseoaa.projectoaa.ui.screen.home

import androidx.compose.animation.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suseoaa.projectoaa.presentation.home.HomeViewModel
import com.suseoaa.projectoaa.ui.component.OaaMarkdownText
import com.suseoaa.projectoaa.ui.component.common.SharedTransitionPageContainer
import com.suseoaa.projectoaa.ui.animation.sharedBoundsTransition
import com.suseoaa.projectoaa.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/**
 * 部门详情页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartmentDetailScreen(
    departmentName: String,
    onBack: () -> Unit,
    onNavigateToEdit: () -> Unit,
    viewModel: HomeViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 加载详情
    LaunchedEffect(departmentName) {
        viewModel.fetchDetailInfo(departmentName)
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .sharedBoundsTransition("department_$departmentName"),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            departmentName,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = uiState.canEditCurrent && uiState.detailData != null,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    FloatingActionButton(
                        modifier = Modifier.sharedBoundsTransition("department_edit_$departmentName"),
                        onClick = onNavigateToEdit,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.Edit, "编辑")
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
                    // 加载中
                    uiState.isLoadingDetail && !uiState.isUpdating -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    // 错误
                    uiState.detailError != null -> {
                        ErrorContent(
                            error = uiState.detailError ?: "未知错误",
                            onRetry = { viewModel.fetchDetailInfo(departmentName) },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    // 显示内容
                    uiState.detailData != null -> {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                top = 16.dp,
                                bottom = 88.dp
                            )
                        ) {
                            item {
                                OaaMarkdownText(
                                    markdown = uiState.detailData!!.data,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        lineHeight = 28.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Department full-screen editor with shared transition.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartmentEditScreen(
    departmentName: String,
    onBack: () -> Unit,
    viewModel: HomeViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var shouldCloseAfterSave by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val containerScrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var editorValue by remember(departmentName) { mutableStateOf(TextFieldValue("")) }
    var hasAppliedInitialSelection by remember(departmentName) { mutableStateOf(false) }
    var editorHasFocus by remember { mutableStateOf(false) }

    LaunchedEffect(departmentName) {
        viewModel.fetchDetailInfo(departmentName)
    }

    LaunchedEffect(uiState.isUpdating, uiState.detailError, shouldCloseAfterSave) {
        if (shouldCloseAfterSave && !uiState.isUpdating) {
            if (uiState.detailError == null) {
                onBack()
            }
            shouldCloseAfterSave = false
        }
    }

    LaunchedEffect(departmentName, uiState.isLoadingDetail, uiState.editContent, uiState.isUpdating) {
        if (!hasAppliedInitialSelection && !uiState.isLoadingDetail) {
            val end = uiState.editContent.length
            editorValue = TextFieldValue(
                text = uiState.editContent,
                selection = TextRange(end)
            )
            hasAppliedInitialSelection = true
            focusRequester.requestFocus()
            delay(120)
            bringIntoViewRequester.bringIntoView()
        } else if (hasAppliedInitialSelection && editorValue.text != uiState.editContent && !uiState.isUpdating) {
            val maxIndex = uiState.editContent.length
            val newStart = editorValue.selection.start.coerceIn(0, maxIndex)
            val newEnd = editorValue.selection.end.coerceIn(0, maxIndex)
            editorValue = TextFieldValue(
                text = uiState.editContent,
                selection = TextRange(newStart, newEnd)
            )
        }
    }

    LaunchedEffect(editorValue.selection, editorHasFocus) {
        if (editorHasFocus) {
            delay(40)
            bringIntoViewRequester.bringIntoView()
        }
    }

    SharedTransitionPageContainer(
        transitionKey = "department_edit_$departmentName"
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "编辑$departmentName",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                        }
                    },
                    actions = {
                        TextButton(
                            enabled = !uiState.isUpdating && uiState.editContent.isNotBlank(),
                            onClick = {
                                shouldCloseAfterSave = true
                                viewModel.submitUpdate()
                            }
                        ) {
                            Text("保存", fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                when {
                    uiState.isLoadingDetail && uiState.editContent.isBlank() -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }

                    uiState.detailError != null && uiState.detailData == null -> {
                        ErrorContent(
                            error = uiState.detailError ?: "加载失败",
                            onRetry = { viewModel.fetchDetailInfo(departmentName) },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    else -> {
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                if (uiState.isUpdating) {
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(containerScrollState)
                                        .imePadding()
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    TextField(
                                        value = editorValue,
                                        onValueChange = { value ->
                                            editorValue = value
                                            viewModel.onEditContentChange(value.text)
                                            if (editorHasFocus) {
                                                scope.launch {
                                                    delay(30)
                                                    bringIntoViewRequester.bringIntoView()
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 420.dp)
                                            .focusRequester(focusRequester)
                                            .bringIntoViewRequester(bringIntoViewRequester)
                                            .onFocusChanged { state ->
                                                editorHasFocus = state.isFocused
                                                if (state.isFocused) {
                                                    scope.launch {
                                                        delay(30)
                                                        bringIntoViewRequester.bringIntoView()
                                                    }
                                                }
                                            },
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            disabledContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent
                                        ),
                                        placeholder = {
                                            Text(
                                                "在此输入 Markdown 内容...",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                                            lineHeight = 26.sp
                                        ),
                                        minLines = 18,
                                        maxLines = Int.MAX_VALUE
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

/**
 * 错误内容
 */
@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Text(
            "加载失败",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onRetry) {
            Text("重试")
        }
    }
}

