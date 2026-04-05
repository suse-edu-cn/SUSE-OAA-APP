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
            // Shared module
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
        versionCode = 112334
        versionName = "1.123.34"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        //仅针对release变体执行逻辑，避免debug包也被误复制到桌面
        if (variant.name.contains("release", ignoreCase = true)) {

            //动态获取当前变体的版本名称，用于后续重命名
            val currentVersionName = android.defaultConfig.versionName ?: "unknown"

            val variantNameCapitalized = variant.name.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }

            //为当前变体注册一个专属的Copy任务
            val copyTask = tasks.register<Copy>("copy${variantNameCapitalized}ApkToDesktop") {
                //设置任务分组与描述
                group = "build"
                description = "将签名后的${variant.name}版本APK重命名并复制到桌面"

                //通过artifacts.get(SingleArtifact.APK)动态获取AGP构建流水线中的APK文件夹对象
                from(variant.artifacts.get(SingleArtifact.APK)) {
                    include("**/*.apk")
                }

                //彻底禁止复制空文件夹(例如可能被连带扫描到的baselineProfiles等目录)
                includeEmptyDirs = false

                //动态获取macOS用户目录并指向桌面文件夹
                val userHome = System.getProperty("user.home")
                val desktopTargetDir = file("$userHome/Desktop/青蟹安装包")
                into(desktopTargetDir)

                //将文件路径扁平化，去除所有源目录层级，确保APK直接放在目标文件夹最外层
                eachFile {
                    //这里的上下文this已经被自动指定为FileCopyDetails，直接调用其内部方法即可
                    path = name
                }

                //按照“青蟹-版本-apk”的规范进行重命名，这里将未使用的fileName参数使用_替代消除警告
                rename { _ ->
                    "青蟹-v$currentVersionName.apk"
                }
            }

            //使用matching与configureEach过滤并配置系统原生的打包任务
            tasks.matching {
                it.name == "assemble$variantNameCapitalized" || it.name == "package$variantNameCapitalized"
            }.configureEach {
                finalizedBy(copyTask)
            }
        }
    }
}
