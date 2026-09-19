import com.android.build.api.dsl.ApplicationExtension
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

extensions.configure<ApplicationExtension>("android") {
    namespace = "com.suseoaa.projectoaa"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.suseoaa.projectoaa"
        minSdk = 28
        targetSdk = 37
        versionCode = 202530
        versionName = "2.25.30"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.add("arm64-v8a")
        }

    }

    signingConfigs {
        create("release") {
            val ciStoreFile = System.getenv("KEYSTORE_FILE_PATH")

            // 签名库路径从 local.properties 的 STORE_FILE 读取，不再硬编码某台开发机的
            // 绝对路径——否则除作者本人外没人能出 release 包。保留旧路径作为兜底默认值，
            // 这样现有本地环境不配置也能继续用。
            val localProps = Properties()
            val propsFile = rootProject.file("local.properties")
            if (propsFile.exists()) {
                FileInputStream(propsFile).use { stream -> localProps.load(stream) }
            }
            val localStoreFilePath = localProps.getProperty("STORE_FILE")
                ?: "${System.getProperty("user.home")}/Desktop/SUSE-APP-Key/APP-Key.jks"

            if (!ciStoreFile.isNullOrEmpty()) {
                // CI 环境
                storeFile = file(ciStoreFile)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            } else if (file(localStoreFilePath).exists()) {
                // 本地环境
                storeFile = file(localStoreFilePath)
                storePassword = localProps.getProperty("STORE_PASSWORD", "")
                keyAlias = localProps.getProperty("KEY_ALIAS", "")
                keyPassword = localProps.getProperty("KEY_PASSWORD", "")
            }
        }
    }

    buildTypes {
        debug {
//            isMinifyEnabled = true
//            proguardFiles(
//                getDefaultProguardFile("proguard-android-optimize.txt"),
//                "proguard-rules.pro",
//                project(":composeApp").file("proguard-rules.pro")
//            )
            if (signingConfigs.getByName("release").storeFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                project(":composeApp").file("proguard-rules.pro")
            )
            if (signingConfigs.getByName("release").storeFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }


    // KMP 库模块的 assets 在当前工程结构下未自动并入 APK，
    // 这里显式加入 composeApp 的 androidMain/assets 目录以确保 ddddocr 模型可用。
    sourceSets {
        getByName("main") {
            assets.directories.add("src/main/assets")
            assets.directories.add(project(":composeApp").file("src/androidMain/assets").path)
        }
    }
}

dependencies {
    implementation(project(":composeApp"))

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

configurations.all {
    exclude(group = "dev.chrisbanes.material3", module = "material3-window-size-class-multiplatform")
}


