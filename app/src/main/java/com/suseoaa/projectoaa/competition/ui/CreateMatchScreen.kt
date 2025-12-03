package com.suseoaa.projectoaa.competition.ui

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suseoaa.projectoaa.competition.viewmodel.CreateMatchViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMatchScreen(
    viewModel: CreateMatchViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.isSubmitSuccess, viewModel.errorMessage) {
        if (viewModel.isSubmitSuccess) {
            snackbarHostState.showSnackbar("比赛创建成功！")
            onBack()
            viewModel.resetState()
        }
        if (viewModel.errorMessage != null) {
            snackbarHostState.showSnackbar("错误: ${viewModel.errorMessage}")
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("创建新比赛") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = viewModel.title,
                onValueChange = { viewModel.title = it },
                label = { Text("比赛标题") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = viewModel.description,
                onValueChange = { viewModel.description = it },
                label = { Text("简短描述") },
                modifier = Modifier.fillMaxWidth()
            )
            //时间选择
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReadOnlyDatePicker(Modifier.weight(1f), "报名开始", viewModel.regStartTime, { viewModel.regStartTime = it }, context)
                ReadOnlyDatePicker(Modifier.weight(1f), "报名结束", viewModel.regEndTime, { viewModel.regEndTime = it }, context)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReadOnlyDatePicker(Modifier.weight(1f), "比赛开始", viewModel.matchStartTime, { viewModel.matchStartTime = it }, context)
                ReadOnlyDatePicker(Modifier.weight(1f), "比赛结束", viewModel.matchEndTime, { viewModel.matchEndTime = it }, context)
            }

            OutlinedTextField(
                value = viewModel.content,
                onValueChange = { viewModel.content = it },
                label = { Text("详细内容 (Markdown)") },
                modifier = Modifier.fillMaxWidth().height(150.dp)
            )

            Button(
                onClick = { viewModel.createMatch() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isLoading
            ) {
                if (viewModel.isLoading) CircularProgressIndicator(Modifier.size(24.dp)) else Text("提交")
            }
        }
    }
}
@Composable
fun ReadOnlyDatePicker(
    modifier: Modifier,
    label: String,
    value: String,
    onDateSelected: (String) -> Unit,
    context: Context
) {
    Box(modifier = modifier.clickable {
        val c = Calendar.getInstance()
        DatePickerDialog(context, { _, y, m, d ->
            onDateSelected(String.format("%d-%02d-%02d", y, m + 1, d))
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }) {
        OutlinedTextField(
            value = value, onValueChange = {}, label = { Text(label) },
            readOnly = true, enabled = false, modifier = Modifier.fillMaxWidth(),
            trailingIcon = { Icon(Icons.Default.DateRange, null) },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}