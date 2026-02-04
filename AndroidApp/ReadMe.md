# PPNNI Mobile Client — Android JNI Secure Inference Simulator

## 🌐 Overview
The **PPNNI Mobile Client** is the Android-based frontend for the Privacy-Preserving Neural Network Inference (SecFePAS) system. It manages the full secure inference lifecycle: from server discovery via the Orchestrator to the execution of native C++ cryptographic protocols via the Java Native Interface (JNI).

This application demonstrates the system's ability to perform high-latency, multi-round secure computation on mobile hardware without compromising on performance or security architecture.

---

## 🧩 Architecture & Directory Layout
The application uses a **Layered Architecture** to bridge the high-level Kotlin UI with the low-level C++ protocol engine.

<pre>

AndroidApp/app/src/
├── main/
│   ├── java/com/example/androidapp/
│   │   ├── MainActivity.kt          # UI Controller & Workflow management
│   │   ├── ClientManager.kt         # API Client & JNI execution wrapper
│   │   ├── NativeBridge.kt          # JNI method declarations
│   │   ├── MockImageInputManager.kt # Workspace management for JNI I/O
│   │   └── ImagePreprocessor.kt     # Bitmap-to-FixedPoint (.inp) conversion
│   ├── cpp/
│   │   ├── client_jni.cpp           # C++ JNI Implementation (The Bridge)
│   │   └── CMakeLists.txt           # NDK Build configuration (statically links Mock Core)
│   └── assets/                      # App resources
└── androidTest/
    └── java/com/example/androidapp/
        └── AndroidAppTest.kt        # Automated Instrumentation Test Suite
     
</pre>

## 🚀 Features

- **Dynamic Orchestration:** Fetches dedicated model server instances from the Kotlin Server Manager via HTTP.
- **Native JNI Integration:** Compiles and executes verified C++20 code directly on the mobile CPU (ARM64).
- **Fixed-Point Preprocessing:** Converts standard mobile images into normalized fixed-point shares (.inp) compatible with MPC protocols.
- **Live Lifecycle Logging:** Displays round-by-round protocol progress (e.g., "Round 1/52 ok") directly in a color-coded, scrollable UI.
- **Result Parsing:** Implements ArgMax logic on native output shares to display deterministic pain-class predictions.



## ⚙️ Components
<pre>
| File                   | Responsibility                                                                        |
|------------------------|---------------------------------------------- ----------------------------------------|
| `MainActivity.kt`      | Orchestrates user input, image selection, and UI log updates.                         |
| `ClientManager.kt`     | Handles the handoff from HTTP (discovery) to TCP (secure socket).                     |
| `NativeBridge.kt`      | JNI wrapper for C++ Wrapper                                                           |
| `ImagePreprocessor.kt` | Normalizes images (Mean/StdDev) and quantizes to 12-bit fixed-point.                  |
| `client_jni.cpp`       | A lean bridge that invokes the verified client_run function from the C++ Mock.        |
| `CMakeLists.txt`       | Statically links the external PPNNI.Mock/mock_client.cpp source for binary stability. |
</pre>

## 🧩 How to Use

1. Run your backend (kotlin/ktor/C++ server) accessible via http request
2. Build and install the Android app. 

```bash 
./gradlew clean
./gradlew assembleDebug
./gradlew connectedAndroidTest
```



3. Enter the Server URL (e.g., http://192.168.1.249:8080) and select model and protocol.
4. Connect to Server.
5. Select an image to start secure inference.
6. Observe the color-coded logs and output in the UI.
- ![Android App (communicating with Server Manager)](/media/androidapp.png)