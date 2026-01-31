plugins {
    kotlin("jvm") version "2.2.20"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core:2.3.5")
    implementation("io.ktor:ktor-server-netty:2.3.5")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.5")
    implementation("io.ktor:ktor-serialization-jackson:2.3.5")
    implementation("io.ktor:ktor-server-openapi:2.3.5")
    implementation("io.ktor:ktor-server-swagger:2.3.5")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
}


application {
    mainClass.set("com.example.server.ServerAppKt")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain(21)
}