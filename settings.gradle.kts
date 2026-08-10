pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}

rootProject.name = "MichisPlayer"
include(":app", ":domain", ":data", ":playback", ":core:common", ":core:ui")
include(":feature:library", ":feature:player", ":feature:bookdetails")
include(":feature:bookmarks", ":feature:settings")
