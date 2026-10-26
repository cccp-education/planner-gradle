@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention").version("1.0.0")
    id("com.gradleup.nmcp.settings").version("1.5.0")
}

val globalProps = java.util.Properties().also {
    val globalFile = file(System.getProperty("user.home") + "/.gradle/gradle.properties")
    if (globalFile.exists()) it.load(globalFile.inputStream())
}

nmcpSettings {
    centralPortal {
        // CI / fresh checkout — credentials are optional at configuration time.
        // A GitHub runner has no ~/.gradle/gradle.properties; hard-failing here
        // kills every invocation (`./gradlew build` included) before a single
        // test runs. Only a real publish needs the values.
        username = globalProps.getProperty("ossrhUsername") ?: ""
        password = globalProps.getProperty("ossrhPassword") ?: ""
        publishingType = "AUTOMATIC"
    }
}

dependencyResolutionManagement {
    // S-082 — the conventions plugin adds project repositories (mavenLocal
    // first). Force the settings repositories to win so the local build resolves
    // the *published* N0/N1 jars, exactly like the CI checkout.
    repositoriesMode.set(org.gradle.api.initialization.resolve.RepositoriesMode.PREFER_SETTINGS)
    repositories {
        // A stale `~/.m2` copy of `vibecoding-contracts:0.0.1` (newer binary API
        // than the Central jar) shadowed the published one and produced a runtime
        // NoSuchMethodError. CI has no mavenLocal; the local build must match it.
        mavenCentral()
    }
}

rootProject.name = "planner-plugin"
