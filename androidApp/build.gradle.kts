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
    ndkVersion = "27.0.12077973"

    defaultConfig {
        applicationId = "com.suseoaa.projectoaa"
        minSdk = 28
        targetSdk = 37
        versionCode = 202227
        versionName = "2.22.27"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.add("arm64-v8a")
        }

    }

    signingConfigs {
        create("release") {
            val ciStoreFile = System.getenv("KEYSTORE_FILE_PATH")
            val localStoreFilePath = "/Users/vincent/Desktop/SUSE-APP-Key/APP-Key.jks"

            if (!ciStoreFile.isNullOrEmpty()) {
                // CI 环境
                storeFile = file(ciStoreFile)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            } else if (file(localStoreFilePath).exists()) {
                // 本地环境
                val localProps = Properties()
                val propsFile = rootProject.file("local.properties")
                if (propsFile.exists()) {
                    val fis = FileInputStream(propsFile)
                    fis.use { stream -> localProps.load(stream) }
                }
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

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
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


