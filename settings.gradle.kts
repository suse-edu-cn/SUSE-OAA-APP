pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ProjectOAA"

// 分层模块：core 为最底层，不依赖任何业务模块
include(":core:common")
include(":core:database")
include(":core:datastore")
include(":core:designsystem")
include(":core:navigation")
include(":core:network")
include(":core:platform")

// feature 模块：依赖 core 与 shared，彼此之间不互相依赖
include(":feature:recruitment")
include(":feature:person")
include(":feature:academic")
include(":feature:account")
include(":feature:update")
include(":feature:checkin")
include(":feature:course")
include(":feature:teachingplan")
include(":feature:home")
include(":feature:grades")
include(":feature:exam")
include(":feature:gpa")

include(":composeApp")
include(":shared")
include(":androidApp")
