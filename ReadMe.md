# Privacy-Preserving Neural Network Inference (PPNNI) System
**Dynamic Orchestration of Secure Inference Servers for Mobile Clients**

## 🌐 Overview

This system enables privacy-preserving neural network inference (PPNNI) across distributed devices using a server–client architecture.

It consists of:

- Server Manager (Kotlin API) – manages the lifecycle of server binaries (model servers).

- Model Server Binary (e.g., OpenCheetah) – runs a privacy-preserving inference protocol for a given neural network model.

- Client Binary (e.g., OpenCheetah client) – connects to the spawned model server to perform encrypted inference on input data (e.g., images).

- Android Mobile App – provides a user-friendly interface to request a server instance and execute secure inference locally via JNI.

**Together, these components allow multiple users to perform secure, on-demand model inference without exposing private input data or model parameters.**


## 🧩 System Architecture
<pre>
        ┌──────────────────────────────┐
        │      Android Mobile App      │
        │  ─────────────────────────── │
        │  UI: Select Model + Image    │
        │  ClientManager:              │
        │  - Request Server from API   │
        │  ┌─────────────────────────┐ │
        │  │ Run Client Binary (JNI) │ │
        │  └─────────────────────────┘ │
        └──────────────┬───────────────┘
                       │ HTTP
                       ▼
        ┌──────────────────────────────┐
        │        Server Manager        │
        │   (Kotlin + Ktor REST API)   │
        │                              │
        │  /getServer?model=XYZ        │
        │    → Allocates port, spawns  │
        │      Model Server process    │
        │      and returns IP + port   │
        │                              │
        │  /status                     │
        │    → Returns server states   │
        └──────┬───────────────────────┘
               │
      ┌────────┴────────┐
      │                 │
      ▼                 ▼
┌─────────────┐   ┌─────────────┐
│ ModelServer │   │ ModelServer │
│ (Port 6000) │   │ (Port 6001) │
└─────────────┘   └─────────────┘

* Each handles one client inference request.
After completion or timeout → terminated.

</pre>


## 🔁 Workflow


**Step 1 — Request Server**

The Android app sends a request to:

`GET /getServer?model=default`


The Server Manager selects a free port (e.g., 6000) and launches a model server process:

`./server 6000`

Server Manager responds with:

```json
{
  "ip": "192.168.1.249",
  "port": 6000,
  "model": "default",
  "status": "ok"
}
```

**Step 2 — Run Client Inference**

The mobile app receives the response, and its ClientManager executes:

`NativeBridge.execute("192.168.1.249", 6000, "image.jpg")`

JNI calls the native client binary, which securely connects to the server and performs encrypted inference.

**Step 3 — Cleanup and Lifecycle**

Each model server runs only for a fixed lifetime (serverLifetimeMs).

After timeout or process completion, Server Manager kills the server and releases the port.

## 🧰 Building and Running
### 🖥️ Server Manager

```bash
cd ServerManager
./gradlew build
./gradlew run
```

### 📱 Android Client

- Open the Android project in Android Studio.
- Build & install on your device.
- Provide the Server Manager URL (e.g., http://192.168.1.249:8080).
- Choose model and image.
- Run inference.

for More information 
- [ServerManager](/ServerManager/ReadMe.ServerManager.md) 
- [AndroidApp](/AndroidApp/ReadMe.AndroidApp.md) 
- [PPNNI](/PPNNI.Dummy/ReadMe.PPNNI.md) 

### Sample Runs
- ![PPNNI (Server and Client)](/img/ppnni.png)
- ![Server Manager (with PPNNI client only)](/img/servermanager.png)
- ![Android App (communicating with Server Manager)](/img/androidapp.png)