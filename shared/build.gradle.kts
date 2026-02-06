import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
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
            baseName = "Shared"
            isStatic = true
            // 链接 SQLite 库
            linkerOpts("-lsqlite3")
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Coroutines
            implementation(libs.kotlinx.coroutines.core)

            // Serialization
            implementation(libs.kotlinx.serialization.json)

            // DateTime
            implementation(libs.kotlinx.datetime)

            // Ktor Client
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.auth)

            // SQLDelight
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)

            // Koin DI
            implementation(libs.koin.core)

            // DataStore
            implementation(libs.androidx.datastore.preferences.core)

            // HTML Parsing - TODO: 待 ksoup 支持 Kotlin 2.1.0 后启用
            // implementation(libs.ksoup)

            // Logging
            implementation(libs.napier)

            // Cryptography
            implementation(libs.cryptography.core)
        }

        androidMain.dependencies {
            // Ktor Android Engine
            implementation(libs.ktor.client.okhttp)

            // SQLDelight Android Driver
            implementation(libs.sqldelight.android.driver)

            // Coroutines Android
            implementation(libs.kotlinx.coroutines.android)

            // Koin Android
            implementation(libs.koin.android)

            // Cryptography Provider
            implementation(libs.cryptography.provider.jdk)
        }

        iosMain.dependencies {
            // Ktor iOS Engine
            implementation(libs.ktor.client.darwin)

            // SQLDelight iOS Driver
            implementation(libs.sqldelight.native.driver)

            // Cryptography Provider
            implementation(libs.cryptography.provider.apple)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "com.suseoaa.projectoaa.shared"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

sqldelight {
    databases {
        create("AppDatabase") {
            packageName.set("com.suseoaa.projectoaa.shared.cache")
        }
    }
}
