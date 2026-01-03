package com.suseoaa.projectoaa.feature.academicPortal

sealed interface AcademicPortalEvent {
    data class NavigateTo(val destination: AcademicDestinations) : AcademicPortalEvent
}