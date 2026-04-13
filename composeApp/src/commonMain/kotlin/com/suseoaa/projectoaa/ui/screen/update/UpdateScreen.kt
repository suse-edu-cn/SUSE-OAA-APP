package com.suseoaa.projectoaa.ui.screen.update

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import com.suseoaa.projectoaa.data.repository.GithubRelease
import com.suseoaa.projectoaa.presentation.update.AppUpdateViewModel
import com.suseoaa.projectoaa.presentation.update.getAppVersionName
import com.suseoaa.projectoaa.presentation.update.isIosPlatform
import com.suseoaa.projectoaa.ui.animation.sharedBoundsTransition
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateScreen(
    onNavigateBack: () -> Unit,
    viewModel: AppUpdateViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val allReleases by viewModel.allReleases.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchAllReleases()
        viewModel.checkForUpdateAuto()
    }

    Scaffold(
        modifier = Modifier.sharedBoundsTransition("update"),
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .padding(8.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), CircleShape)
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val isTablet = maxWidth > 800.dp

            if (isTablet) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        HeroVersionSection(uiState.hasUpdate, uiState.latestRelease, isTablet = true)
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
                            allReleases = allReleases,
                            isChecking = uiState.isChecking,
                            downloadingReleaseTag = uiState.downloadingReleaseTag,
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
                        HeroVersionSection(uiState.hasUpdate, uiState.latestRelease, isTablet = false)
                    }

                    item {
                        Text(
                            text = "Release Notes",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(start = 24.dp, top = 32.dp, bottom = 16.dp)
                        )
                    }

                    if (allReleases.isEmpty() && uiState.isChecking) {
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
                        items(allReleases) { release ->
                            Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                                ReleaseCard(
                                    release = release,
                                    isCurrentVersion = getAppVersionName() == release.tagName.removePrefix("v"),
                                    downloadingReleaseTag = uiState.downloadingReleaseTag,
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
        Box(
            modifier = Modifier
                .size(if (isTablet) 140.dp else 120.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ),
                    shape = RoundedCornerShape(if (isTablet) 40.dp else 36.dp)
                )
                .padding(4.dp)
                .clip(RoundedCornerShape(if (isTablet) 36.dp else 32.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Info, // Placeholder for App Icon
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(if (isTablet) 64.dp else 56.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

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
                    Icon(
                        Icons.Default.Info, // Check icon or similar
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
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
    isChecking: Boolean,
    downloadingReleaseTag: String?,
    viewModel: AppUpdateViewModel
) {
    Column(modifier = Modifier.fillMaxSize()) {
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
                    ReleaseCard(
                        release = release,
                        isCurrentVersion = getAppVersionName() == release.tagName.removePrefix("v"),
                        downloadingReleaseTag = downloadingReleaseTag,
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
    isCurrentVersion: Boolean,
    downloadingReleaseTag: String?,
    viewModel: AppUpdateViewModel
) {
    val isThisReleaseDownloading = downloadingReleaseTag == release.tagName

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
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = release.tagName,
                    style = MaterialTheme.typography.headlineSmall,
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

            com.mikepenz.markdown.m3.Markdown(
                content = release.body,
                modifier = Modifier.fillMaxWidth()
            )

            if (!isIosPlatform()) {
                val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
                if (apkAsset != null) {
                    Spacer(modifier = Modifier.height(28.dp))
                    if (isThisReleaseDownloading) {
                        OutlinedButton(
                            onClick = { viewModel.cancelDownload() },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
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
                                modifier = Modifier.weight(1f).height(50.dp),
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
                                modifier = Modifier.weight(1f).height(50.dp),
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
            }
        }
    }
}