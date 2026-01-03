package com.suseoaa.projectoaa.feature.academicPortal

sealed interface AcademicPortalEvent {
//    跳转到成绩
    data object ToGrades: AcademicPortalEvent
}