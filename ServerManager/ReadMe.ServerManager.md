# 🔧 Server Manager (for PPNNI Server)  

A lightweight **model server orchestrator** written in **Kotlin + Ktor**, designed to dynamically allocate ports and launch secure model processes (e.g., inference servers) upon client requests. In this example we used [PPNNI.Dummy](../PPNNI.Dummy/) and here is the [ReadMe.PPNNI](../PPNNI.Dummy/ReadMe.PPNNI.md) 

---

## 🚀 Features
- Dynamically spawns subprocesses for models on demand  
- Automatically manages process lifetime (kills inactive ones)  
- JSON-based configuration (`config.json`)  
- REST API endpoints:
  - `GET /getServer?model=<model>` → allocates a port and launches model
  - `GET /status` → shows running servers and their status

### 📂 Directory Layout (aprt fron auto generated)
<pre>
ServerManager
├──/src/main/kotlin
│   ├── ServerApp.kt         → Application entrypoint
│   ├── ServerController.kt  → Defines Ktor routes (/getServer, /status)
│   ├── ServerService.kt     → Business logic (spawning, tracking, cleanup)
│   ├── Config.kt            → Configuration and model definitions
├── config.json
├──.rest
├── README.ServerManager.md
└── xxxx
</pre>


## ⚙️ Configuration

Create a `config.json` file in the same directory:

```json
{
  "hostIp": "192.168.1.249",
  "managerPort": 8080,
  "portPool": [6000, 6001, 6002],
  "serverLifetimeMs": 300000,
  "models": {
    "default": {
      "model_dir": "/home/user/models/default",
      "model_cmd": "./run_model.sh"
    }
  }
}
```

## 📦 Build & Run

```bash
./gradlew build
./gradlew run
```

Server will start at:
`hostIp:managerPort` e.g. http://192.168.1.249:8080

## 🔍 API Example
```rest
GET http://192.168.1.249:8080/getServer?model=default
###
```
```json
{
  "ip": "192.168.1.249",
  "port": 6000,
  "model": "default",
  "status": "ok"
}

```rest
GET http://192.168.1.249:8080/status
###
```
```json
[
  {
    "port": 6000,
    "running": true,
    "model": "default",
    "startedAt": "2025-10-24T22:06:30.230890Z"
  },
  { ... },
  ...
]
```

## 📚 Dependencies

- Kotlin 1.9+
- Ktor 2.3+
- Jackson Kotlin Module
- kotlinx.coroutines 
