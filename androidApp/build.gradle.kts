import com.android.build.api.dsl.ApplicationExtension

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
        versionCode = 202226
        versionName = "2.22.26"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.add("arm64-v8a")
        }

    }

    signingConfigs {
        create("release") {
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
        debug {
//            isMinifyEnabled = true
//            proguardFiles(
//                getDefaultProguardFile("proguard-android-optimize.txt"),
//                "proguard-rules.pro",
//                project(":composeApp").file("proguard-rules.pro")
//            )
            signingConfig = signingConfigs.getByName("release")
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                project(":composeApp").file("proguard-rules.pro")
            )
            signingConfig = signingConfigs.getByName("release")
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


