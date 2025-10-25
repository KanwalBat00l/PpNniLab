# About LICORacii deliverables
1. Projects
## 1.1.  Server Manager
Create Server Manager

```bash 
mkdir ServerManager
cd ServerManager

mkdir -p src/main/kotlin
touch src/main/kotlin/ServerManager.kt
touch build.gradle.kts
touch settings.gradle.kts
touch config.json
```

```css
ServerManager/
 ├─ build.gradle.kts
 ├─ settings.gradle.kts
 ├─ config.json
 └─ src/
    └─ main/
       └─ kotlin/
          └─ ServerManager.kt
```

> settings.gradle.kts
```kotlin
rootProject.name = "ServerManager"
```

> build.gradle.kts

```kotlin
plugins {
    kotlin("jvm") version "1.9.23"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-netty:2.3.5")
    implementation("io.ktor:ktor-server-core:2.3.5")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("ch.qos.logback:logback-classic:1.4.11")
}

application {
    mainClass.set("ServerManagerKt")
}
```
> config.json

```json
{
  "hostIp": "127.0.0.1",
  "managerPort": 8080,
  "portPool": [6000,6001,6002],
  "serverLifetimeMs": 120000,
  "models": {
    "default": "./server"
  }
}
```
> Run 
 `gradle wrapper` to generate 
```
gradlew
gradlew.bat
/gradle/wrapper/gradle-wrapper.properties
```
> Run 
`chmod +x gradlew`

> Run the API
`./gradlew run`

