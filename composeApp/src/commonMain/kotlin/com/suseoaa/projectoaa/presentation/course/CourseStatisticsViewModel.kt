package com.suseoaa.projectoaa.presentation.course

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.data.repository.LocalCourseRepository
import com.suseoaa.projectoaa.shared.domain.model.course.CourseAccountEntity
import com.suseoaa.projectoaa.shared.domain.model.course.CourseWithTimes
import com.suseoaa.projectoaa.shared.data.repository.GradeEntity
import com.suseoaa.projectoaa.shared.data.repository.SchoolGradeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class CourseNodeData(
    val courseName: String,
    val studentName: String,
    val primaryGrade: GradeEntity? = null,
    val makeupGrade: GradeEntity? = null
)

class CourseStatisticsViewModel(
    private val localRepository: LocalCourseRepository,
    private val courseViewModel: CourseViewModel,
    private val gradeRepository: SchoolGradeRepository
) : ViewModel() {

    // 所有的系统账号
    val allAccounts: StateFlow<List<CourseAccountEntity>> = courseViewModel.savedAccounts

    // 默认选用当前账号
    private val _selectedAccountIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedAccountIds: StateFlow<Set<String>> = _selectedAccountIds.asStateFlow()

    // 选中的学期集合。空集合表示“全部学期”
    private val _selectedTerms = MutableStateFlow<Set<Pair<String, String>>>(emptySet())
    val selectedTerms: StateFlow<Set<Pair<String, String>>> = _selectedTerms.asStateFlow()

    // 搜索关键字
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // 聚合当前选中账号拥有的所有学期对 (xnm, xqm)
    @OptIn(ExperimentalCoroutinesApi::class)
    val availableTerms: StateFlow<List<Pair<String, String>>> = _selectedAccountIds
        .flatMapLatest { ids ->
            if (ids.isEmpty()) {
                flowOf(emptyList())
            } else {
                val flows = ids.map { localRepository.getAvailableTerms(it) }
                combine(flows) { termsLists ->
                    termsLists.flatMap { it }.distinct().sortedWith(compareByDescending<Pair<String, String>> { it.first }.thenByDescending { it.second })
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 根据选中的账号和学期，聚合出的课程数据
    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredCourses: StateFlow<List<CourseWithTimes>> = combine(
        _selectedAccountIds,
        _selectedTerms
    ) { ids, terms ->
        Pair(ids, terms)
    }.flatMapLatest { (ids, terms) ->
        if (ids.isEmpty()) {
            flowOf(emptyList())
        } else {
            val flows = ids.map { studentId ->
                if (terms.isEmpty()) {
                    localRepository.getAllCoursesByStudent(studentId)
                } else {
                    val termFlows = terms.map { localRepository.getCourses(studentId, it.first, it.second) }
                    if (termFlows.isEmpty()) flowOf(emptyList()) else combine(termFlows) { it.flatMap { list -> list } }
                }
            }
            combine(flows) { coursesList ->
                // 去重
                coursesList.flatMap { it }.distinctBy { 
                    "${it.course.studentId}_${it.course.courseName}_${it.times.joinToString { t -> t.uniqueId.toString() }}"
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 聚合所有选中账号的全量成绩
    @OptIn(ExperimentalCoroutinesApi::class)
    private val allGrades: StateFlow<List<GradeEntity>> = _selectedAccountIds
        .flatMapLatest { ids ->
            if (ids.isEmpty()) {
                flowOf(emptyList())
            } else {
                val flows = ids.map { gradeRepository.observeAllGrades(it) }
                combine(flows) { gradesLists -> gradesLists.flatMap { it } }
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 针对时间线视图的数据结构：按学生分组 -> 按学期分组 -> 按老师分组 -> CourseNodeData
    @OptIn(ExperimentalCoroutinesApi::class)
    val timelineData: StateFlow<Map<String, Map<Pair<String, String>, Map<String, List<CourseNodeData>>>>> = combine(
        filteredCourses, allGrades, _searchQuery
    ) { courses, grades, query ->
        val queryLower = query.trim().lowercase()
        courses.groupBy { it.course.studentId }
            .mapValues { (studentId, studentCourses) ->
                studentCourses.groupBy { Pair(it.course.xnm, it.course.xqm) }
                    .entries.sortedWith(compareBy<Map.Entry<Pair<String, String>, *>> { it.key.first }.thenBy { it.key.second })
                    .associate { it.key to it.value }
                    .mapValues { (_, coursesInTerm) ->
                        val teacherMap = mutableMapOf<String, MutableSet<CourseNodeData>>()
                        coursesInTerm.forEach { c ->
                            val courseName = c.course.courseName
                            val studentName = courseViewModel.savedAccounts.value.find { it.studentId == studentId }?.name ?: studentId
                            
                            val teachers = c.times.map { it.teacher.trim() }.filter { it.isNotEmpty() }.toSet()
                            val teacherKeys = if (teachers.isEmpty()) setOf("未知教师") else teachers
                            
                            // 应用搜索过滤：如果名称或任一教师包含查询词
                            val matchSearch = queryLower.isEmpty() || 
                                courseName.lowercase().contains(queryLower) || 
                                teacherKeys.any { it.lowercase().contains(queryLower) }
                                
                            if (matchSearch) {
                                // 查找该课程的所有成绩（通过名称匹配，实现跨学期归宗）
                                val courseGrades = grades.filter { it.studentId == studentId && it.courseName == courseName }
                                val makeupGrade = courseGrades.find { it.examNature.contains("补考") }
                                val primaryGrade = courseGrades.find { !it.examNature.contains("补考") } ?: courseGrades.firstOrNull()
                                
                                val nodeData = CourseNodeData(courseName, studentName, primaryGrade, makeupGrade)
                                
                                teacherKeys.forEach { teacher ->
                                    teacherMap.getOrPut(teacher) { mutableSetOf() }.add(nodeData)
                                }
                            }
                        }
                        teacherMap.mapValues { it.value.toList() }.filterValues { it.isNotEmpty() }
                    }.filterValues { it.isNotEmpty() }
            }.filterValues { it.isNotEmpty() }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    fun syncAllData() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val currentYear = com.suseoaa.projectoaa.shared.util.OaaClock.now()
                    .toLocalDateTime(TimeZone.currentSystemDefault()).year
                
                _selectedAccountIds.value.forEach { studentId ->
                    val account = courseViewModel.savedAccounts.value.find { it.studentId == studentId } ?: return@forEach
                    
                    // 1. 同步所有成绩
                    gradeRepository.fetchAllHistoryGrades(account)
                    
                    // 2. 同步所有课程
                    val startYear = account.njdmId.toIntOrNull() ?: (currentYear - 4)
                    val endYear = currentYear + 1
                    for (year in startYear..endYear) {
                        listOf("3", "12").forEach { semester ->
                            courseViewModel.fetchAndSaveCourseSchedule(account.studentId, account.password, year.toString(), semester)
                        }
                    }
                }
            } finally {
                _isSyncing.value = false
            }
        }
    }

    init {
        // 初始化时，如果 selectedAccountIds 为空，则自动选中第一个账号
        viewModelScope.launch {
            courseViewModel.savedAccounts.collect { accounts ->
                if (accounts.isNotEmpty() && _selectedAccountIds.value.isEmpty()) {
                    _selectedAccountIds.value = setOf(accounts.first().studentId)
                }
            }
        }
    }

    fun toggleAccount(studentId: String) {
        val current = _selectedAccountIds.value.toMutableSet()
        if (current.contains(studentId)) {
            if (current.size > 1) { // 保证至少选中一个
                current.remove(studentId)
            }
        } else {
            current.add(studentId)
        }
        _selectedAccountIds.value = current
    }

    fun toggleTerm(xnm: String, xqm: String) {
        _selectedTerms.value = _selectedTerms.value.toMutableSet().apply {
            val term = Pair(xnm, xqm)
            if (contains(term)) remove(term) else add(term)
        }
    }

    fun selectAllTerms() {
        _selectedTerms.value = emptySet()
    }
}
