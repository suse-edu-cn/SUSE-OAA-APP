package com.suseoaa.projectoaa.feature.academicPortal.getGrades

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suseoaa.projectoaa.core.network.model.academic.studentGrade.StudentGradeResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradesScreen(
    viewModel: GradesViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val grades by viewModel.grades.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.loadGrades()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("成绩查询") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SelectOption()
            if (grades.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无数据或正在加载...")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(grades) { item ->
                        GradeItemCard(item)
                    }
                }
            }
        }
    }
}

@Composable
fun GradeItemCard(item: StudentGradeResponse.Item) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                item.kcmc?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = "${item.cj}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    LabelValueText("学分", item.xf)
                    LabelValueText("绩点", item.jd)
                }
                Column(horizontalAlignment = Alignment.End) {
                    LabelValueText("类型", item.kcxzmc)
                    LabelValueText("考核", item.khfsmc)
                }
            }
        }
    }
}

@Composable
fun LabelValueText(label: String, value: String?) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun SelectOption() {
    data class YearOption(
        val labelOfYear: String,
        val valueOfYear: String,
    )


    val startYear = 2023
    val yearOptions = remember(startYear) {
        List(4) { index ->
            val current = startYear + index
            val next = current + 1
            YearOption(
                labelOfYear = "$current-$next 学年",
                valueOfYear = current.toString()
            )
        }
    }


    var expandedOfYear by remember { mutableStateOf(false) }
    var selectedYearOption by remember { mutableStateOf(yearOptions[0]) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row() {
            Row(
                modifier = Modifier
                    .clickable { expandedOfYear = true }
                    .padding(8.dp)
            ) {
                Text(selectedYearOption.labelOfYear)
                Icon(Icons.Default.ArrowDropDown, contentDescription = "下拉")
            }

            DropdownMenu(
                expanded = expandedOfYear,
                onDismissRequest = { expandedOfYear = false },
                modifier = Modifier
                    .background(color = MaterialTheme.colorScheme.background)
            ) {
                yearOptions.forEach {
                    DropdownMenuItem(
                        text = { Text(text = it.labelOfYear) },
                        onClick = {
                            selectedYearOption = it
                            expandedOfYear = false
                        }
                    )
                }
            }
        }
        Row() {
            data class SemesterOption(
                val labelOfSemester: String,
                val valueOfSemester: String
            )

            val semesterOption = listOf(
                SemesterOption(labelOfSemester = "上学期", valueOfSemester = "3"),
                SemesterOption(labelOfSemester = "下学期", valueOfSemester = "12")
            )
            var expandedOfSemester by remember { mutableStateOf(false) }
            var selectedSemesterOption by remember { mutableStateOf(semesterOption[0]) }
            Row(
                modifier = Modifier
                    .clickable { expandedOfSemester = true }
                    .padding(8.dp)
            ) {
                Text(selectedSemesterOption.labelOfSemester)
                Icon(Icons.Default.ArrowDropDown, contentDescription = "下拉")
            }
            DropdownMenu(
                expanded = expandedOfSemester,
                onDismissRequest = { expandedOfSemester = false },
                modifier = Modifier
                    .background(color = MaterialTheme.colorScheme.background)
            ) {
                semesterOption.forEach {
                    DropdownMenuItem(
                        text = { Text(text = it.labelOfSemester) },
                        onClick = {
                            selectedSemesterOption = it
                            expandedOfSemester = false
                        }
                    )
                }
            }
        }
    }
}