package com.suseoaa.projectoaa.feature.academicPortal.getExamInfo

import com.suseoaa.projectoaa.core.data.repository.SchoolInfoRepository
import com.suseoaa.projectoaa.core.database.dao.CourseDao
import com.suseoaa.projectoaa.core.database.entity.CourseAccountEntity
import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.core.ui.BaseInfoViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GetExamInfoViewModel @Inject constructor(
    private val repository: SchoolInfoRepository,
    courseDao: CourseDao,
    tokenManager: TokenManager
) : BaseInfoViewModel<List<ExamUiState>>(tokenManager, courseDao) {

    init {
        fetchData()
    }

    override suspend fun executeRequest(account: CourseAccountEntity): Result<List<ExamUiState>> {
        val result = repository.getAcademicExamInfo(account)

        return result.map { stringList ->
            stringList.map { rawString ->
                // 格式：课程名###时间###地点
                val parts = rawString.split("###")

                val name = parts.getOrElse(0) { "未知课程" }
                val time = parts.getOrElse(1) { "时间待定" }
                val location = parts.getOrElse(2) { "地点待定" }

                ExamUiState(
                    courseName = name,
                    time = time,
                    location = location
                )
            }
        }
    }
}