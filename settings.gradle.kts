@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        id("education.cccp.codebase") version "0.0.1"
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
