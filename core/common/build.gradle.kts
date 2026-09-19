import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

kotlin {
    jvmToolchain(25)

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "com.suseoaa.projectoaa.core.common"
        compileSdk = 37
        minSdk = 28

        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget("25"))
        }

        withHostTest { }
    }

    // 库模块只需要产出 klib，framework 由 composeApp 统一打包
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.datetime)
            // HTML 解析（教务系统返回的是网页而非 JSON）
            implementation(libs.ksoup)
            // 日志后端，通过 AppLog 收口，不对外暴露
            implementation(libs.napier)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
