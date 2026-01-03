package com.suseoaa.projectoaa.feature.academicPortal

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.data.repository.CourseRepository
import com.suseoaa.projectoaa.core.data.repository.SchoolRepository
import com.suseoaa.projectoaa.core.database.entity.CourseAccountEntity
import com.suseoaa.projectoaa.core.network.model.academic.studentGrade.StudentGradeResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AcademicViewModel @Inject constructor(
    private val schoolRepository: SchoolRepository,
    private val localRepository: CourseRepository
) : ViewModel() {
    private val _grades = MutableStateFlow<List<StudentGradeResponse.Item>>(emptyList())
    val grades = _grades.asStateFlow()
    val currentAccount: StateFlow<CourseAccountEntity?> = localRepository.allAccounts
        .map { accounts -> accounts.firstOrNull() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun loadGrades() {
        viewModelScope.launch {
            val accounts = localRepository.allAccounts.first()
            val account = accounts.firstOrNull()
            if (account != null) {
                // 打印日志确认
                println("当前登录账号: ${account.name}")
                val result = schoolRepository.getGrades(account, "2025", "3")
                result.onSuccess { list ->
                    _grades.value = list
                }.onFailure { e ->
                    println("获取成绩失败：${e.message}")
                }
            } else {
                println("当前没有登录账号")
            }
        }
    }
}