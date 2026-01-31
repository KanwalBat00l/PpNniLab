# Android Client (for PPNNI Client)

A lightweight Android application that connects to a backend server to perform **privacy-preserving neural network inference** through a **native C++ client**.


## 🧩 Architecture
<pre>
AndroidApp/app/src/main/
├──/java
│   ├── MainActivity.kt          → Application entrypoint
│   ├── ClientManager.kt         → for HTTP and TCP communications between client and server
│   ├── NativeBridge.kt          → Bridge between Java and Cpp
│   └── ImagePreprocessor.kt     → Image preprocessing to inp format
├──/cpp
│   ├── client_jni.cpp
│   └── CMakeList.txt
├──...

</pre>


## 🚀 Features

- Connects to a backend server for model, protocol selection and secure inference.
- Allows dynamic input of backend URL and model name.
- Displays live logs in a color-coded, scrollable UI.
- Previews selected image before sending.
- Executes a native C++ client via JNI and shows complete output logs.




## ⚙️ Components

| File                | Responsibility                                |
|---------------------|-----------------------------------------------|
| `MainActivity.kt`   | Handles UI, user interaction, and log updates |
| `ClientManager.kt`  | Business logic: server discovery, JNI calls   |
| `NativeBridge.kt`   | JNI wrapper for C++ Wrapper                   |
| `client_jni.cpp`    | cpp wrapper for client_android                |
| `client_android.so` | Native binary performing secure inference     |


## 🧩 How to Use

1. Run your backend (kotlin/ktor/C++ server) accessible via http request
2. Build and install the Android app. 

```bash 
./gradlew clean
./gradlew assembleDebug
```

3. Enter the Server URL (e.g., http://192.168.1.249:8080) and select model and protocol.
4. Connect to Server.
5. Select an image to start secure inference.
6. Observe the color-coded logs and output in the UI.
- ![Android App (communicating with Server Manager)](/media/androidapp.png)