import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

plugins {
    alias(libs.plugins.publish)
    id("education.cccp.build.gradle-plugin") version "0.0.1"
    id("education.cccp.build.publishing") version "0.0.1"
    kotlin("plugin.serialization") version libs.versions.kotlin.get()
}

group = "education.cccp"
version = libs.plugins.planner.get().version

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(gradleApi())
    implementation(gradleKotlinDsl())
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.langchain4j.core)
    implementation(libs.langchain4j.ollama)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.kotlin)
    implementation(libs.koog.agents)

    // N0 codebase contracts — source unique de vérité (ContextChannel, ChannelBudget, CompositeContext, CompositeContextConfig)
    implementation("education.cccp:codebase-contracts:0.0.1")

    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    jvmArgs("-XX:+EnableDynamicAgentLoading")
    testLogging {
        exceptionFormat = FULL
    }
}

gradlePlugin {
    website.set("https://github.com/cheroliv/planner-gradle/")
    vcsUrl.set("https://github.com/cheroliv/planner-gradle.git")

    plugins {
        create("planner") {
            id = libs.plugins.planner.get().pluginId
            implementationClass = "planning.PlanningPlugin"
            displayName = "Planner Plugin"
            description = """
                Planning Expert — decomposes a high-level intention (natural language)
                into a structured execution plan (EPICs → User Stories → Gradle tasks)
                via LLM (LangChain4j + DeepSeek-v4-pro). Outputs structured stdout.
            """.trimIndent()
            tags.set(listOf("planning", "llm", "langchain4j", "agile", "backlog"))
        }
    }
}

publishingConventions {
    publicationType = "PLUGIN"
}

publishing {
    publications {
        withType<MavenPublication> {
            pom {
                name.set("Planner Gradle Plugin")
                description.set(gradlePlugin.plugins.getByName("planner").description)
            }
        }
    }
    repositories {
        mavenCentral()
    }
}