package com.suseoaa.projectoaa.feature.academicPortal.getExamInfo

import com.suseoaa.projectoaa.core.data.repository.SchoolRepository
import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.core.database.dao.CourseDao
import com.suseoaa.projectoaa.core.database.entity.CourseAccountEntity
import com.suseoaa.projectoaa.core.ui.BaseInfoViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GetExamInfoViewModel @Inject constructor(
    private val schoolRepository: SchoolRepository,
    private val courseDao: CourseDao,
    private val tokenManager: TokenManager
) : BaseInfoViewModel<List<ExamUiState>>(tokenManager, courseDao) {
    override suspend fun executeRequest(account: CourseAccountEntity): Result<List<ExamUiState>> {
        val rawResult = schoolRepository.getAcademicExamInfo(account)

        // 转换：String -> ExamUiState
        return rawResult.map { list ->
            list.map { rawString -> parseExamString(rawString) }
        }
    }

    fun parseExamString(rawString: String): ExamUiState {
        // 原始字符串示例: "2025-2026-1-网络安全技术-2026-01-08(09:30-11:30)-LA5-322"

        // 正则解释：
        // ^.*?-\d-          -> 忽略开头的 "2025-2026-1-"
        // (.+?)             -> 捕获组1：课程名 (网络安全技术)，非贪婪匹配
        // -                 -> 分隔符
        // (\d{4}-\d{2}-\d{2}\(.*\)) -> 捕获组2：时间 (2026-01-08(09:30-11:30))
        // -                 -> 分隔符
        // (.+)$             -> 捕获组3：地点 (LA5-322)，直到字符串结束
        val regex = Regex("""^.*?-\d-(.+?)-(\d{4}-\d{2}-\d{2}\(.*\))-(.+)$""")

        val matchResult = regex.find(rawString)

        return if (matchResult != null) {
            val (name, time, loc) = matchResult.destructured
            ExamUiState(
                courseName = name,
                time = time,
                location = loc
            )
        } else {
            // 如果格式不对（比如没抓到），为了不崩，返回原始字符串方便调试
            ExamUiState(courseName = rawString)
        }
    }
}


data class ExamUiState(
    val courseName: String = "", // 课程名
    val time: String = "",       // 考试时间
    val location: String = ""    // 考试地点
)