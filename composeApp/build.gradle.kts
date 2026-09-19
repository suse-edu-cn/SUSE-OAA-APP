@file:Suppress("DEPRECATION")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(25)

    // 抑制 expect/actual 类的 Beta 警告
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "com.suseoaa.projectoaa.composeapp"
        compileSdk = 37
        minSdk = 28

        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget("25"))
        }

        // 打开 JVM 单元测试源集。此前 composeApp 完全没有测试源集，
        // 所有 ViewModel 与 presentation 层逻辑都无处可测。
        withHostTest { }
    }

    listOf(
        // iosX64 (Intel 模拟器) 已移除：Miuix (top.yukonga.miuix.kmp) 没有发布该架构的构件，
        // 而现代开发机基本都是 Apple Silicon，用 iosSimulatorArm64 即可。
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            // 链接 SQLite 库
            linkerOpts("-lsqlite3")
            export(project(":shared"))
            export(libs.kotlinx.datetime)
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Haze库，可以提供毛玻璃效果
            implementation(libs.haze)
            // 提供类似 iOS 和 Windows 预设材质效果的扩展库
            implementation(libs.haze.materials)
            // Shared模块
            api(project(":shared"))
            // 主题、通用组件与液态玻璃渲染
            api(project(":core:designsystem"))
            // 平台能力：OCR、下载、权限、Toast、设备信息
            api(project(":core:platform"))
            api(project(":core:navigation"))
            // 业务功能模块
            implementation(project(":feature:recruitment"))
            implementation(project(":feature:person"))
            implementation(project(":feature:academic"))
            implementation(project(":feature:account"))
            implementation(project(":feature:update"))
            implementation(project(":feature:checkin"))
            implementation(project(":feature:course"))
            implementation(project(":feature:teachingplan"))
            implementation(project(":feature:home"))
            implementation(project(":feature:grades"))
            implementation(project(":feature:exam"))
            implementation(project(":feature:gpa"))

            // Compose Multiplatform
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            // Lifecycle & ViewModel (KMP)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)

            // Navigation (KMP) - 需要 2.8.0+
            implementation(libs.androidx.navigation.compose)

            // Ktor Networking
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)

            // Koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // Image Loading
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)

            // Markdown
            implementation(libs.multiplatform.markdown.renderer.m3)

            // Serialization
            implementation(libs.kotlinx.serialization.json)

            // Coroutines
            api(libs.kotlinx.coroutines.core)

            // DateTime
            api(libs.kotlinx.datetime)
            // KMP DataStore
            implementation(libs.androidx.datastore.preferences.core)
            // Window Size Class
            implementation(libs.material3.windowSize)

            // HTML Parsing - KSoup (KMP alternative to Jsoup)
            implementation(libs.ksoup)

            // Miuix for BottomBar liquid glass —— 该库发布了 iosArm64/iosSimulatorArm64
            // 构件，是真正的 KMP 库；放在 commonMain 让 Android/iOS 共用同一份液态玻璃实现。
            implementation(libs.miuix.blur)
            implementation(libs.miuix.ui)
        }

        androidMain.dependencies {
            // Android specific
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.koin.android)

            // Lifecycle Process (用于 App 生命周期监听)
            implementation("androidx.lifecycle:lifecycle-process:2.8.5")

            // Compose Preview
            implementation(compose.preview)

            // Jetpack Glance (App Widgets)
            implementation(libs.androidx.glance.appwidget)
            implementation(libs.androidx.glance.material3)

            // Ktor Android Engine
            implementation(libs.ktor.client.okhttp)

            // ML Kit Text Recognition (验证码识别)
            implementation(libs.mlkit.text.recognition)
            implementation(libs.mlkit.text.recognition.chinese)

            // ONNX Runtime (ddddocr 移植)
            implementation(libs.onnxruntime.android)
        }

        iosMain.dependencies {
            // Ktor iOS Engine
            implementation(libs.ktor.client.darwin)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            // ViewModel 测试要用 runTest 与可控调度器
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}


