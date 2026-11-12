// Stempeln - Projekt-Einstellungen (Version siehe app/build.gradle.kts)
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositories { google(); mavenCentral() }
}
rootProject.name = "stempeln"
include(":logik", ":app")
