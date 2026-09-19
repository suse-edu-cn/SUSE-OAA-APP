import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(25)

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "com.suseoaa.projectoaa.shared"
        // 与 composeApp / androidApp 保持一致；此前是 36，三个模块编译用的
        // android.jar 不同版本，属于容易埋雷的不一致。
        compileSdk = 37
        minSdk = 28

        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget("25"))
        }

        // 打开 JVM 侧的单元测试。此前 commonTest 只在 iOS 模拟器目标上编译运行，
        // 意味着这些测试只有 macOS 开发机跑得动，Linux CI 上等于没有测试。
        withHostTest { }
    }

    listOf(
        // iosX64 (Intel 模拟器) 已移除：Miuix (top.yukonga.miuix.kmp) 没有发布该架构的构件，
        // 而现代开发机基本都是 Apple Silicon，用 iosSimulatorArm64 即可。
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
            // 链接 SQLite 库
            linkerOpts("-lsqlite3")
        }
    }

    sourceSets {
        commonMain.dependencies {
            // 基础工具与错误模型；用 api 暴露，让上层模块透传拿到
            api(project(":core:common"))
            api(project(":core:datastore"))
            api(project(":core:database"))
            api(project(":core:network"))

            // Coroutines
            api(libs.kotlinx.coroutines.core)

            // Serialization
            api(libs.kotlinx.serialization.json)

            // DateTime
            api(libs.kotlinx.datetime)

            // Ktor Client
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.network)

            // Koin DI
            implementation(libs.koin.core)

            // DataStore
            implementation(libs.androidx.datastore.preferences.core)

            // HTML Parsing
            implementation(libs.ksoup)

            // Logging
            implementation(libs.napier)

            // Cryptography
            implementation(libs.cryptography.core)
        }

        androidMain.dependencies {
            // Ktor Android Engine
            implementation(libs.ktor.client.okhttp)

            // Coroutines Android
            implementation(libs.kotlinx.coroutines.android)

            // Koin Android
            implementation(libs.koin.android)

            // Cryptography Provider
            implementation(libs.cryptography.provider.jdk)
        }

        iosMain.dependencies {
            // Ktor iOS Engine
            implementation(libs.ktor.client.darwin)

            // Cryptography Provider
            implementation(libs.cryptography.provider.apple)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

