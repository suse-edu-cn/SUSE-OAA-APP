import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}
android {
    namespace = "com.suseoaa.projectoaa"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.suseoaa.projectoaa"
        minSdk = 28
        targetSdk = 36
        versionCode = 1288
        versionName = "1.28.8"

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
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.foundation)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
//    测量窗口的大小，用于适配多设备的比例
    implementation(libs.androidx.compose.material3.window.size)
//    MD3图标包
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.compose.material.icons.extended)
//    viewmodel相关的库
    implementation(libs.androidx.lifecycle.viewmodel.compose)
//    导航相关的库
    implementation(libs.androidx.navigation.compose)
    // 网络请求相关依赖
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    // Hilt 依赖
    implementation(libs.hilt.android)
    // 使用 ksp 处理注解
    ksp(libs.hilt.compiler)

    // Compose 专用：让你能使用 hiltViewModel()
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.volley)
    implementation(libs.play.services.maps)
//    DataStore相关依赖
    implementation(libs.androidx.datastore.preferences)
//    动画
    implementation(libs.androidx.compose.animation)
//    解析请求的HTML代码
    implementation(libs.jsoup)
//    Coil图片库
    implementation(libs.coil.compose)
//    md解析器
    implementation(libs.multiplatform.markdown.renderer.m3)
//    图片压缩
    implementation(libs.compressor)}