@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        // Aucun plugin education.cccp requis — vibecoding contracts N0, pas codebase N1
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention").version("1.0.0") }

rootProject.name = "planner-gradle"
include(":planner-plugin")
