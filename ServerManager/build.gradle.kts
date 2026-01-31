plugins {
    // Explicitly using 2.1.0 to resolve the 'HasConvention' error in Gradle 9
    kotlin("jvm") version "2.1.0"
    application
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Ktor Server 2.3.12
    val ktorVersion = "2.3.12"
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-swagger-jvm:$ktorVersion")
    
    // JSON
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
    
    // Testing (JUnit 5)
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}

application {
    // Points to ServerApp.kt in the root of src/main/kotlin
    mainClass.set("ServerAppKt")
}

tasks.test {
    useJUnitPlatform() // Required to run JUnit 5
    // This makes the output professional and verbose
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true // Shows println() from tests
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

kotlin {
    jvmToolchain(21)
}