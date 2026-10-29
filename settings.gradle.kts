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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Кандзи дэс нэ"
include(":app")

include(":domain:handwriting", ":core:writingpad")

include(":domain:practice", ":feature:practice", ":data:kanjivg", ":core:designsystem")

include(":checker:shape", ":checker:writing", ":checker:readability", ":checker:strokes")

include(":feature:dashboard")

include(":feature:settings")

include(":data:settings")

include(":domain:catalog", ":data:catalogdb", ":feature:catalog", ":core:wordui", ":feature:dictionary")
