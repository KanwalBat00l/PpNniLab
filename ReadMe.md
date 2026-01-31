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
┌─────────────────────────────────────────┐        ┌─────────────────────────────────────────┐
│             Android Mobile App          │        │              Server Manager             │
│ ─────────────────────────────────────── │        │     (Always Running, Known Port)        │
│ UI: Select Model + Protocol + Image     │        │ ──────────────────────────────────────  │
│ ClientManager                           │ <----->│ GET /getServer?model=X&protocol=Y       │
│   • Sends HTTP request                  │  HTTP  │   → Picks free port                     │
│   • Receives IP + Port + Model + Proto  │        │   → Spawns new PPNNI Server             │
│                                         │        │   → Returns {ip, port, model, protocol} │
│  ┌───────────────────────────────┐      │        │                                         │
│  │          PPNNI Client         │      │        └─────────────────────────────────────────┘
│  │   (Native Client via JNI)     │      │                                │
│  └───────────────^───────────────┘      │                       ┌────────┴────────┐ 
└──────────────────│──────────────────────┘                       ▼                 ▼
                   │                                      ┌─────────────┐   ┌─────────────┐
                   └────────────────────────────────── >  │ ModelServer │   │ ModelServer │
                         TCP Direct Communication         │ (Port 6000) │   │ (Port 6001) │
                                                          └─────────────┘   └─────────────┘
                                                          * Each handles one client inference request.
                                                          After completion or timeout → terminated.
</pre>


## 🔁 Workflow


**Step 1 — Request Server**

The Android app sends a request to:

`GET /getServer?model=resnet50&protocol=cheetah`
> sported models are    → resnet50, resnet50_quantized, sqnet, sqnet_quantized
> sported protocols are → cheetah, SCI_HE


The Server Manager selects a free port (e.g., 6000) and launches a model server process:

`./server 6000`

Server Manager responds with:

```json
HTTP/1.1 200 OK
Content-Type: application/json
Connection: close
transfer-encoding: chunked

{
  "ip": "192.168.1.249",
  "port": 9181,
  "model": "resnet50",
  "protocol": "cheetah",
  "status": "ok"
}
```

**Step 2 — Run Client Inference**

The mobile app receives the response, 
Lets user select an image, and preprocess it to convert to .inp format, 
the mobile app clientManager executes:

`NativeBridge.execute( "resnet50", "cheetah", "192.168.1.249", 6000)`

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
- [PPNNI](/PPNNI.Mock/ReadMe.PPNNI.md) 

### Testing PPNNI Client and Server
- **Step 1: Start the Server**  using `bash scripts/run-server.sh [cheetah|SCI_HE] [resnet50|sqnet|resnet50_quantized|sqnet_quantized] <port>`
- **Step 2: Run the Client** using `bash scripts/run-client.sh [cheetah|SCI_HE] [resnet50|sqnet|resnet50_quantized|sqnet_quantized] <server_ip> <port>`

### Sample Runs
- ![PPNNI (Server and Client)](/media/ppnni.png)
- ![Server Manager (swagger api)](/media/swagger1.png)
- ![Server Manager (swagger api)](/media/swagger2.png)
- ![Android App (communicating with Server Manager)](/media/androidapp.png)