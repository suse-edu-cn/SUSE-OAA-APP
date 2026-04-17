import com.android.build.api.dsl.ApplicationExtension

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

extensions.configure<ApplicationExtension>("android") {
    namespace = "com.suseoaa.projectoaa"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.suseoaa.projectoaa"
        minSdk = 28
        targetSdk = 36
        versionCode = 113044
        versionName = "1.130.44"
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
            signingConfig = signingConfigs.getByName("release")
        }

        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                project(":composeApp").file("proguard-rules.pro")
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion("25")
        targetCompatibility = JavaVersion.toVersion("25")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":composeApp"))

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}




