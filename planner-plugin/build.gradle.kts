import org.gradle.api.JavaVersion.VERSION_24
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

plugins {
    signing
    `java-library`
    `maven-publish`
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.publish)
    kotlin("plugin.serialization") version libs.versions.kotlin.get()
}

group = "education.cccp"
version = libs.plugins.planner.get().version
kotlin.jvmToolchain(24)

repositories {
    mavenLocal()
    mavenCentral()
}

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
    useJUnitPlatform()
    jvmArgs("-XX:+EnableDynamicAgentLoading")
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
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

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        withType<MavenPublication> {
            pom {
                name.set("Planner Gradle Plugin")
                description.set(gradlePlugin.plugins.getByName("planner").description)
                url.set(gradlePlugin.website.get())
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("cccp-education")
                        name.set("CCCP Education")
                        email.set("cccp.education@gmail.com")
                    }
                }
                scm {
                    connection.set(gradlePlugin.vcsUrl.get())
                    developerConnection.set(gradlePlugin.vcsUrl.get())
                    url.set(gradlePlugin.vcsUrl.get())
                }
                project.findProperty("relocationGroup")?.let { targetGroup ->
                    withXml {
                        val pom = asElement()
                        val doc = pom.ownerDocument
                        val distMgmt = doc.createElement("distributionManagement")
                        val relocation = doc.createElement("relocation")
                        relocation.appendChild(doc.createElement("groupId")).also { it.textContent = targetGroup.toString() }
                        relocation.appendChild(doc.createElement("artifactId")).also { it.textContent = project.name }
                        distMgmt.appendChild(relocation)
                        pom.appendChild(distMgmt)
                    }
                }
            }
        }
    }
    repositories {
        mavenCentral()
    }
}

signing {
    if (System.getenv("CI") != "true" && !version.toString().endsWith("-SNAPSHOT")) {
        sign(publishing.publications)
    }
    useGpgCmd()
}
