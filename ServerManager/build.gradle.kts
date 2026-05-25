plugins {
    kotlin("jvm") version "2.1.0"
    application
    // Added these two for D6.2 requirements
    id("com.gradleup.shadow") version "8.3.3" 
    `maven-publish`
}

group = "eu.licorice" // Updated for your project
version = "1.0.0"     // Version tracking as requested by partners

repositories {
    mavenCentral()
}

dependencies {
    val ktorVersion = "2.3.12"
    
    // Core Ktor Libraries
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-swagger-jvm:$ktorVersion")
    
    // JSON & Formatting
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
    
    // Standard Logging (Logback)
    implementation("ch.qos.logback:logback-classic:1.4.14")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}

application {
    mainClass.set("ServerAppKt")
}

// Configuration for uploading to Nexus
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            // This tells Gradle to upload the "Fat JAR" (the one with all dependencies)
            artifact(tasks.named("shadowJar"))
            groupId = "eu.licorice"
            artifactId = "ppnni-server-manager"
            version = "1.0.0"
        }
    }
    repositories {
        maven {
            name = "Nexus"
            url = uri("https://newregistry.evidenresearch.eu/repository/LICORICE-maven/")
            credentials {
                // We use project properties so we don't hardcode passwords here
                username = project.findProperty("nexusUsername")?.toString() ?: ""
                password = project.findProperty("nexusPassword")?.toString() ?: ""
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

kotlin { jvmToolchain(21) }