package com.suseoaa.projectoaa.feature.academicPortal.getCourseInfo

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.suseoaa.projectoaa.feature.home.MessageInfo
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

@Composable
fun GetCourseInfo(viewModel: GetCourseInfoViewModel = hiltViewModel()) {
    val courseInfo = viewModel.fetchAcademicCourseInfo().toString().trimIndent()
    val doc: Document = Jsoup.parse(courseInfo)
    val academicCourseChangeInfo = doc.select("div.list-group.dtsx a[data-tkxx]")
        .map { it.attr("data-tkxx") }
    LazyColumn(
        modifier = Modifier
            .padding(16.dp)
    ) {
        items(academicCourseChangeInfo) {
            Text(it)
        }
    }
}