import com.android.build.api.artifact.SingleArtifact
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    // 抑制 expect/actual 类的 Beta 警告
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            // 链接 SQLite 库
            linkerOpts("-lsqlite3")
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Haze库，可以提供毛玻璃效果
            implementation(libs.haze)
            // 提供类似 iOS 和 Windows 预设材质效果的扩展库
            implementation(libs.haze.materials)
            // Shared模块
            implementation(project(":shared"))

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
            implementation(libs.kotlinx.coroutines.core)

            // DateTime
            implementation(libs.kotlinx.datetime)
            // KMP DataStore
            implementation(libs.androidx.datastore.preferences.core)
            // Window Size Class
            implementation(libs.material3.windowSize)

            // HTML Parsing - KSoup (KMP alternative to Jsoup)
            implementation(libs.ksoup)
        }

        androidMain.dependencies {
            // Android specific
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.koin.android)

            // Compose Preview
            implementation(compose.preview)

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
        }
    }
}

android {
    namespace = "com.suseoaa.projectoaa"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.suseoaa.projectoaa"
        minSdk = 28
        targetSdk = 36
        versionCode = 113042
        versionName = "1.130.42"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // 从环境变量中读取
            val storeFilePathStr = System.getenv("KEYSTORE_FILE_PATH")
            if (!storeFilePathStr.isNullOrEmpty()) {
                storeFile = file(storeFilePathStr)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 绑定签名配置
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Debug
    debugImplementation(compose.uiTooling)
}


//利用androidComponentsAPI在构建配置阶段对变体进行拦截与处理
androidComponents {
    //遍历项目中所有的变体(如debug,release等)
    onVariants { variant ->
        // 如果你需要未来增加其它变体的自定义处理，可以保留该空块或删除它
    }
}
