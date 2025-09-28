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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven {url = uri("https://artifact.bytedance.com/repository/Volcengine/")}
    }
}

rootProject.name = "GraceWord"
include(":app-gw",
    ":chat-sdk-vendor",
    ":sdk-guru-common",
    ":vendor-chatkit",
    ":chat-sdk-firebase-push",
    "chat-sdk-core")
 