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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AcademicViewModel @Inject constructor(
    application: Application,
    private val schoolRepository: SchoolRepository
) : ViewModel() {
    private val _grades = MutableStateFlow<List<StudentGradeResponse.Item>>(emptyList())
    val grades = _grades.asStateFlow()

    private val _currentAccount = MutableStateFlow<CourseAccountEntity?>(null)
    val currentAccount = _currentAccount.asStateFlow()

    fun loadGrades(){
        viewModelScope.launch {
            if (currentAccount!=null){
                val result = schoolRepository.getGrades(currentAccount,"2025","3")
            }
        }
    }
}