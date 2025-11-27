import java.io.FileInputStream
import java.util.Properties
plugins {
    // 步骤1-1：添加 Android 与 Kotlin 插件
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // 步骤1-2：添加 KSP（Room 编译器使用 KSP 生成代码）——版本与 Kotlin 对齐（来自版本库）
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.suseoaa.projectoaa"
    compileSdk =36

    defaultConfig {
        applicationId = "com.suseoaa.projectoaa"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"


        val properties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(FileInputStream(localPropertiesFile))
        }

        val baseUrl = properties.getProperty("BASE_URL") ?: "\"http://172.27.25.195:8080/\""

        buildConfigField("String", "API_BASE_URL", baseUrl)
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.exifinterface)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // 步骤1-3：添加 Room 依赖（runtime + ktx + 编译器）
    // runtime 提供运行时，ktx 提供协程/Flow 支持，ksp 触发注解处理生成 DAO 实现
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    // 编译器版本与 runtime 对齐（此处使用 2.8.3）
    ksp(libs.androidx.room.compiler)

    // Retrofit + OkHttp
    implementation(libs.retrofit)
    implementation(libs.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    implementation(libs.moshi.kotlin)
    // Moshi codegen（生成适配器，零反射）
    ksp(libs.moshi.kotlin.codegen)
//    页面跳转
    implementation(libs.androidx.navigation.compose)


    // Material3 WindowSizeClass 支持
    implementation(libs.androidx.compose.material3.window.size.class1)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    // 图标包
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.material.icons.extended)

    // Coil 图片加载库
    implementation(libs.coil.compose)
}