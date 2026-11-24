package com.suseoaa.projectoaa.ui.screen.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.shared.domain.model.person.CurrentUser
import com.suseoaa.projectoaa.shared.domain.permission.OaaPermission
import com.suseoaa.projectoaa.ui.component.common.PullUpFeatureDrawer
import com.suseoaa.projectoaa.ui.component.FeatureCard

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeWithDrawer(
    currentUser: CurrentUser?,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onNavigateToRecruitment: () -> Unit,
    onNavigateToUserQuery: () -> Unit,
    onNavigateToOrganization: () -> Unit,
    onNavigateToAppManagement: () -> Unit = {},
    onNavigateToValueCalculator: () -> Unit,
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

            item(key = "feature_value_calculator") {
                FeatureCard(
                    name = "物品价值计算",
                    icon = Icons.Default.Calculate,
                    color = MaterialTheme.colorScheme.surface,
                    onColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        onExpandedChange(true)
                        onNavigateToValueCalculator()
                    },
                    sharedBoundKey = "value_calculator_feature"
                )
            }

            if (OaaPermission.canAccessUserManagement(currentUser)) {
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

            if (OaaPermission.canManageOrganization(currentUser)) {
                item(key = "feature_organization") {
                    FeatureCard(
                        name = "组织架构",
                        icon = Icons.Default.AccountTree,
                        color = MaterialTheme.colorScheme.surface,
                        onColor = MaterialTheme.colorScheme.primary,
                        onClick = {
                            onExpandedChange(true)
                            onNavigateToOrganization()
                        },
                        sharedBoundKey = "organization_feature"
                    )
                }
            }

            if (OaaPermission.canManageApps(currentUser)) {
                item(key = "feature_app_management") {
                    FeatureCard(
                        name = "应用管理",
                        icon = Icons.Default.Apps,
                        color = MaterialTheme.colorScheme.surface,
                        onColor = MaterialTheme.colorScheme.primary,
                        onClick = {
                            onExpandedChange(true)
                            onNavigateToAppManagement()
                        },
                        sharedBoundKey = "app_management_feature"
                    )
                }
            }
        }
    }
}
