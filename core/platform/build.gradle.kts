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
        namespace = "com.suseoaa.projectoaa.core.platform"
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
            api(project(":core:common"))
            // 预测式返回手势要读应用设置
            implementation(project(":core:datastore"))
            // 端侧模型的下载地址收在 ApiConfig 里
            implementation(project(":core:network"))

            api(compose.runtime)
            api(compose.ui)
            // Toast 浮层用到 Material3 的 Snackbar 样式
            implementation(compose.material3)
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
            // 模型下载走 Ktor
            implementation(libs.ktor.client.core)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
        }

        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation("androidx.lifecycle:lifecycle-process:2.8.5")
            implementation(compose.foundation)
            // 验证码识别：ML Kit 与 ONNX（ddddocr 移植）
            implementation(libs.mlkit.text.recognition)
            implementation(libs.mlkit.text.recognition.chinese)
            implementation(libs.onnxruntime.android)
            implementation(libs.ktor.client.okhttp)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}
