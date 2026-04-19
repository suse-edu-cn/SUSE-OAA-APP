package com.suseoaa.projectoaa.ui.screen.update

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import com.suseoaa.projectoaa.data.repository.GithubAsset
import com.suseoaa.projectoaa.data.repository.GithubRelease
import com.suseoaa.projectoaa.presentation.update.AppUpdateViewModel
import com.suseoaa.projectoaa.presentation.update.getAppVersionName
import com.suseoaa.projectoaa.presentation.update.isIosPlatform
import com.suseoaa.projectoaa.ui.component.common.SharedTransitionPageContainer
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateScreen(
    onNavigateBack: () -> Unit,
    viewModel: AppUpdateViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val allReleases by viewModel.allReleases.collectAsState()
    val isRefreshing = uiState.isChecking

    LaunchedEffect(Unit) {
        viewModel.fetchAllReleases()
        viewModel.checkForUpdateAuto()
    }

    SharedTransitionPageContainer(transitionKey = "update") {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .padding(8.dp)
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    CircleShape
                                )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    viewModel.fetchAllReleases()
                    viewModel.checkForUpdateAuto()
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val isTablet = maxWidth > 800.dp
                    val hasUpdate = uiState.hasUpdate
                    val latestRelease = uiState.latestRelease
                    val releaseNotesReleases = remember(
                        allReleases,
                        hasUpdate,
                        latestRelease?.tagName
                    ) {
                        if (hasUpdate && latestRelease != null) {
                            allReleases.filterNot { it.tagName == latestRelease.tagName }
                        } else {
                            allReleases
                        }
                    }

                    if (isTablet) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = if (hasUpdate) Arrangement.Top else Arrangement.Center
                            ) {
                                HeroVersionSection(
                                    hasUpdate,
                                    latestRelease,
                                    isTablet = true
                                )

                                if (hasUpdate && latestRelease != null) {
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .padding(horizontal = 32.dp)
                                    ) {
                                        ReleaseCard(
                                            release = latestRelease,
                                            isLatestRelease = true,
                                            isCurrentVersion = getAppVersionName() == latestRelease.tagName.removePrefix("v"),
                                            downloadingReleaseTag = uiState.downloadingReleaseTag,
                                            downloadedReleaseTag = uiState.downloadedReleaseTag,
                                            isDownloading = uiState.isDownloading,
                                            downloadProgress = uiState.downloadProgress,
                                            fixedActionButtons = true,
                                            modifier = Modifier.fillMaxHeight(),
                                            viewModel = viewModel
                                        )
                                    }
                                }
                            }

                            VerticalDivider(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(vertical = 32.dp),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .fillMaxHeight()
                            ) {
                                ReleaseHistorySection(
                                    allReleases = releaseNotesReleases,
                                    hasUpdate = false,
                                    latestRelease = latestRelease,
                                    isChecking = uiState.isChecking,
                                    downloadingReleaseTag = uiState.downloadingReleaseTag,
                                    downloadedReleaseTag = uiState.downloadedReleaseTag,
                                    isDownloading = uiState.isDownloading,
                                    downloadProgress = uiState.downloadProgress,
                                    viewModel = viewModel
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            item {
                                HeroVersionSection(
                                    hasUpdate,
                                    latestRelease,
                                    isTablet = false
                                )
                            }

                            if (hasUpdate && latestRelease != null) {
                                item {
                                    Box(
                                        modifier = Modifier.padding(
                                            horizontal = 24.dp,
                                            vertical = 8.dp
                                        )
                                    ) {
                                        ReleaseCard(
                                            release = latestRelease,
                                            isLatestRelease = true,
                                            isCurrentVersion = getAppVersionName() == latestRelease.tagName.removePrefix("v"),
                                            downloadingReleaseTag = uiState.downloadingReleaseTag,
                                            downloadedReleaseTag = uiState.downloadedReleaseTag,
                                            isDownloading = uiState.isDownloading,
                                            downloadProgress = uiState.downloadProgress,
                                            viewModel = viewModel
                                        )
                                    }
                                }
                            }

                            item {
                                Text(
                                    text = "Release Notes",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(
                                        start = 24.dp,
                                        top = 32.dp,
                                        bottom = 16.dp
                                    )
                                )
                            }

                            if (releaseNotesReleases.isEmpty() && uiState.isChecking) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 4.dp
                                        )
                                    }
                                }
                            } else {
                                items(releaseNotesReleases) { release ->
                                    val isLatestRelease = uiState.latestRelease?.tagName == release.tagName
                                    val releaseForDisplay =
                                        if (isLatestRelease)
                                            uiState.latestRelease!!
                                        else
                                            release

                                    Box(
                                        modifier = Modifier.padding(
                                            horizontal = 24.dp,
                                            vertical = 12.dp
                                        )
                                    ) {
                                        ReleaseCard(
                                            release = releaseForDisplay,
                                            isLatestRelease = isLatestRelease,
                                            isCurrentVersion = getAppVersionName() == release.tagName.removePrefix("v"),
                                            downloadingReleaseTag = uiState.downloadingReleaseTag,
                                            downloadedReleaseTag = uiState.downloadedReleaseTag,
                                            isDownloading = uiState.isDownloading,
                                            downloadProgress = uiState.downloadProgress,
                                            viewModel = viewModel
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeroVersionSection(hasUpdate: Boolean, latestRelease: GithubRelease?, isTablet: Boolean) {
    val currentVersion = getAppVersionName()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = if (isTablet) 0.dp else 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "青蟹",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Version $currentVersion",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (hasUpdate && latestRelease != null) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.error, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "发现新版本 ${latestRelease.tagName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        } else {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "已是最新版本",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ReleaseHistorySection(
    allReleases: List<GithubRelease>,
    hasUpdate: Boolean,
    latestRelease: GithubRelease?,
    isChecking: Boolean,
    downloadingReleaseTag: String?,
    downloadedReleaseTag: String?,
    isDownloading: Boolean,
    downloadProgress: Int,
    viewModel: AppUpdateViewModel
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (hasUpdate && latestRelease != null) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .padding(top = 24.dp)
            ) {
                ReleaseCard(
                    release = latestRelease,
                    isLatestRelease = true,
                    isCurrentVersion = getAppVersionName() == latestRelease.tagName.removePrefix("v"),
                    downloadingReleaseTag = downloadingReleaseTag,
                    downloadedReleaseTag = downloadedReleaseTag,
                    isDownloading = isDownloading,
                    downloadProgress = downloadProgress,
                    viewModel = viewModel
                )
            }
        }

        Text(
            text = "Release Notes",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 32.dp, top = 32.dp, bottom = 24.dp)
        )

        if (allReleases.isEmpty() && isChecking) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 32.dp, end = 32.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(allReleases) { release ->
                    val isLatestRelease = latestRelease?.tagName == release.tagName
                    val releaseForDisplay =
                        if (isLatestRelease)
                            latestRelease
                        else
                            release

                    ReleaseCard(
                        release = releaseForDisplay,
                        isLatestRelease = isLatestRelease,
                        isCurrentVersion = getAppVersionName() == release.tagName.removePrefix("v"),
                        downloadingReleaseTag = downloadingReleaseTag,
                        downloadedReleaseTag = downloadedReleaseTag,
                        isDownloading = isDownloading,
                        downloadProgress = downloadProgress,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun ReleaseCard(
    release: GithubRelease,
    isLatestRelease: Boolean,
    isCurrentVersion: Boolean,
    downloadingReleaseTag: String?,
    downloadedReleaseTag: String?,
    isDownloading: Boolean,
    downloadProgress: Int,
    fixedActionButtons: Boolean = false,
    modifier: Modifier = Modifier,
    viewModel: AppUpdateViewModel
) {
    val isThisReleaseDownloading = downloadingReleaseTag == release.tagName
    val isThisReleaseReadyToInstall =
        downloadedReleaseTag == release.tagName && !isDownloading && downloadProgress >= 100
    val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
    val showActionButtons = !isIosPlatform() && apkAsset != null

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentVersion)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCurrentVersion) 0.dp else 4.dp,
            hoveredElevation = 8.dp
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = release.tagName,
                    style = if (isLatestRelease)
                        MaterialTheme.typography.headlineMedium
                    else
                        MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 0.5.sp
                )

                if (isCurrentVersion) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Text(
                            text = "当前版本",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
            )

            if (fixedActionButtons && showActionButtons) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        com.mikepenz.markdown.m3.Markdown(
                            content = release.body,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                ReleaseCardActionButtons(
                    apkAsset = apkAsset,
                    release = release,
                    isThisReleaseReadyToInstall = isThisReleaseReadyToInstall,
                    isThisReleaseDownloading = isThisReleaseDownloading,
                    viewModel = viewModel
                )
            } else {
                com.mikepenz.markdown.m3.Markdown(
                    content = release.body,
                    modifier = Modifier.fillMaxWidth()
                )

                if (showActionButtons) {
                    Spacer(modifier = Modifier.height(28.dp))
                    ReleaseCardActionButtons(
                        apkAsset = apkAsset,
                        release = release,
                        isThisReleaseReadyToInstall = isThisReleaseReadyToInstall,
                        isThisReleaseDownloading = isThisReleaseDownloading,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun ReleaseCardActionButtons(
    apkAsset: GithubAsset?,
    release: GithubRelease,
    isThisReleaseReadyToInstall: Boolean,
    isThisReleaseDownloading: Boolean,
    viewModel: AppUpdateViewModel
) {
    if (apkAsset == null) return

    if (isThisReleaseReadyToInstall) {
        Button(
            onClick = { viewModel.installDownloadedApk() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("安装更新", fontWeight = FontWeight.Bold)
        }
    } else if (isThisReleaseDownloading) {
        OutlinedButton(
            onClick = { viewModel.cancelDownload() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = CircleShape,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("取消下载", fontWeight = FontWeight.Bold)
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = {
                    viewModel.downloadApk(
                        url = apkAsset.downloadUrl,
                        fileName = apkAsset.name,
                        digest = apkAsset.digest,
                        isProxy = false,
                        releaseTag = release.tagName
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            ) {
                Text("直接下载", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    viewModel.downloadApk(
                        url = "https://ghfast.top/${apkAsset.downloadUrl}",
                        fileName = apkAsset.name,
                        digest = apkAsset.digest,
                        isProxy = true,
                        releaseTag = release.tagName
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("加速下载", fontWeight = FontWeight.Bold)
            }
        }
    }
}