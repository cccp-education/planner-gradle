import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

plugins {
    alias(libs.plugins.publish)
    id("education.cccp.build.gradle-plugin") version "0.0.4"
    id("education.cccp.build.publishing") version "0.0.4"
    id("education.cccp.build.cucumber") version "0.0.4"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.10"
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

    // N0 agent contracts — source unique de vérité (Plan, Epic, UserStory, GradleTask, TaskType, AgentState)
    implementation("education.cccp:agent-contracts:0.0.3")

    // N1 codebase — LLM socle (EPIC PLN-LLM-HUB): LlmBuildService + LlmProvider
    implementation(libs.codebase.plugin)

    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    jvmArgs("-XX:+EnableDynamicAgentLoading")
    testLogging {
        exceptionFormat = FULL
    }
}

tasks.named("pluginUnderTestMetadata").configure {
    dependsOn("jar")
}

tasks.named("validatePlugins").configure {
    dependsOn("jar")
}

// Exclude @Tag("integration") tests from the normal `test` task — they
// require a real Ollama instance (ConnectException on localhost:1 in CI).
// Run on demand via `./gradlew :planner-plugin:integrationTest`.
tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("integration")
    }
}

// Dedicated integration test task — runs JUnit tests tagged @Tag("integration")
// (e.g. DecomposeIntentionMultiCanalTest / DecomposeIntentionPluginTest GradleRunner
// scenarios requiring a real Ollama instance). Excluded from `check`; run on demand
// via `./gradlew :planner-plugin:integrationTest`. The cucumber engine is excluded
// so the CucumberTestRunner suite is not picked up.
val integrationTest = tasks.register<Test>("integrationTest") {
    group = "verification"
    description = "Runs integration tests (requires real Ollama)."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    shouldRunAfter(tasks.test)
    useJUnitPlatform {
        includeTags("integration")
        excludeEngines("cucumber")
    }
    filter {
        excludeTestsMatching("planning.steps.CucumberTestRunner")
    }
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