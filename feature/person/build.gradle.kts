import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvmToolchain(25)

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "com.suseoaa.projectoaa.feature.person"
        compileSdk = 37
        minSdk = 28

        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget("25"))
        }

        withHostTest { }
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":shared"))
            implementation(project(":core:designsystem"))
            implementation(project(":core:platform"))

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)

            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.kotlinx.datetime)
            implementation(project(":core:navigation"))
            implementation(libs.coil.compose)
            // 「我的」页聚合了定时签到入口与应用更新，两者的 UI 与状态直接复用其功能模块。
            // 这是刻意保留的 feature 间依赖：该页本身就是一个聚合页，
            // 依赖方向单向（person -> checkin / update），不构成环。
            implementation(project(":feature:checkin"))
            implementation(project(":feature:update"))
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
