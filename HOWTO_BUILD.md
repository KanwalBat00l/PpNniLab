# 🛠 How to Build & Publish (Technical Guide)

### 1. Create the Maven Library (JAR & XML)
1. Navigate to `ServerManager/`.
2. Ensure `gradle.properties` contains Nexus credentials.
3. Run `./gradlew publish`. 
   *Note: This creates the 19MB Fat JAR and the XML POM.*

### 2. Create the Server Side (Docker)
1. Go to the root directory.
2. Run: `docker build --platform linux/amd64 -t newregistry.evidenresearch.eu/licorice/ppnni-server:1.0 .`
3. Run: `docker login newregistry.evidenresearch.eu`
4. Run: `docker push newregistry.evidenresearch.eu/licorice/ppnni-server:1.0`

### 3. Create the Mobile Client (APK)
1. Open `AndroidApp` in Android Studio.
2. Select `Build > Build Bundle(s) / APK(s) > Build APK(s)`.
3. Locate the `app-debug.apk` in the output folder.