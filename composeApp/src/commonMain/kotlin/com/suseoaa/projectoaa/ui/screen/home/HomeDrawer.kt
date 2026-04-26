package com.suseoaa.projectoaa.ui.screen.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.shared.domain.model.person.PersonData
import com.suseoaa.projectoaa.ui.component.common.PullUpFeatureDrawer

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeWithDrawer(
    userInfo: PersonData?,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onNavigateToRecruitment: () -> Unit,
    onNavigateToUserQuery: () -> Unit,
    onNavigateToActivityCheckin: () -> Unit,
    bottomBarHeight: Dp = 0.dp,
    backGestureProgress: Float? = null,
    backGestureCancelCount: Int = 0,
    baseContent: @Composable () -> Unit
) {
    PullUpFeatureDrawer(
        isExpanded = isExpanded,
        onExpandedChange = onExpandedChange,
        title = "应用功能",
        bottomBarHeight = bottomBarHeight,
        backGestureProgress = backGestureProgress,
        backGestureCancelCount = backGestureCancelCount,
        baseContent = baseContent
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item(key = "feature_recruitment") {
                FeatureCard(
                    name = "招新换届",
                    icon = Icons.Default.GroupAdd,
                    color = MaterialTheme.colorScheme.surface,
                    onColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        onExpandedChange(true)
                        onNavigateToRecruitment()
                    },
                    sharedBoundKey = "recruitment_feature"
                )
            }

            item(key = "feature_activity_checkin") {
                FeatureCard(
                    name = "活动签到",
                    icon = Icons.AutoMirrored.Filled.BluetoothSearching,
                    color = MaterialTheme.colorScheme.surface,
                    onColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        onExpandedChange(true)
                        onNavigateToActivityCheckin()
                    },
                    sharedBoundKey = "activity_checkin_feature"
                )
            }

            val invalidRoles = listOf("会员", "普通成员", "")
            if (userInfo != null && userInfo.role !in invalidRoles) {
                item(key = "feature_user_management") {
                    FeatureCard(
                        name = "权利的游戏",
                        icon = Icons.Default.ManageAccounts,
                        color = MaterialTheme.colorScheme.surface,
                        onColor = MaterialTheme.colorScheme.primary,
                        onClick = {
                            onExpandedChange(true)
                            onNavigateToUserQuery()
                        },
                        sharedBoundKey = "user_management_feature"
                    )
                }
            }
        }
    }
}