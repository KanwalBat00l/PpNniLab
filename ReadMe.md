# 🚀 D6.2 Deliverable: Secure Inference Prototype

This branch contains the integration-ready components for the LICORICE project.

## 📂 Components
- **API Orchestrator:** Kotlin/Ktor service (ServerManager).
- **Secure Logic:** C++ PPNNI Mock Server & Client.
- **User Interface:** Android Mobile Application.

## 📋 Deliverable 6.2 Status
- [x] **Swagger API:** Available in `/docs/swagger.json`.
- [x] **GitLab XML:** Maven Project Descriptor available in `/docs/pom.xml`.
- [x] **Hello World Prototype:** Integrated Docker environment.
- [x] **Nexus Readiness:** Build scripts prepared for Maven and Docker registries.

## 🏁 Running the "Hello World" Prototype
1. **Pull the Docker Image** (Server side):
   `docker run -p 8080:8080 -p 9000-9200:9000-9200 <image-url-here>`
2. **Install the Mobile App**: Install the APK provided in the binaries folder.
3. **Configure**: Enter the server IP in the app settings.
4. **Execute**: Click "Get Server" -> "Inference".

-![for more details](ReadMePPNNI.md)