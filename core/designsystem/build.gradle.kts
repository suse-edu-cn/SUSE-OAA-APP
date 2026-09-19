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
        namespace = "com.suseoaa.projectoaa.core.designsystem"
        compileSdk = 37
        minSdk = 28

        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget("25"))
        }
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            // 动画里用到 OaaClock 取时间
            implementation(project(":core:common"))

            // 这些类型出现在组件的公开签名里（Modifier、Color、HazeState 等），用 api 暴露
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api(compose.ui)
            implementation(compose.materialIconsExtended)

            // 毛玻璃与液态玻璃
            api(libs.haze)
            implementation(libs.haze.materials)
            implementation(libs.miuix.blur)
            implementation(libs.miuix.ui)

            // 自适应布局所需的窗口尺寸分类
            api(libs.material3.windowSize)

            // Markdown 渲染（更新日志、AI 回复）
            implementation(libs.multiplatform.markdown.renderer.m3)

            api(libs.kotlinx.coroutines.core)
        }
    }
}
