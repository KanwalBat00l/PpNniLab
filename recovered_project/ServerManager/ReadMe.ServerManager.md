# 🔧 Server Manager (for PPNNI Server)  

A lightweight **model server orchestrator** written in **Kotlin + Ktor**, designed to dynamically allocate ports and launch secure model processes (e.g., inference servers) upon client requests. In this example we used [PPNNI.Mock](../PPNNI.Mock/) and here is the [ReadMe.PPNNI](../PPNNI.Mock/ReadMe.PPNNI.md) 

---

## 🚀 Features
- Dynamically spawns subprocesses for models on demand  
- Automatically manages process lifetime (kills inactive ones)  
- JSON-based configuration (`config.json`)  
- REST API endpoints:
- `GET /getServer?model=resnet50&protocol=cheetah`
  > sported models are    → resnet50, resnet50_quantized, sqnet, sqnet_quantized
  > sported protocols are → cheetah, SCI_HE
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
  "portRange": [9000, 9200],
  "serverLifetimeMs": 120000,
  "models": {
    "resnet50": {
      "model_dir": "../PPNNI.Mock",
      "model_cmd": "scripts/run-server.sh"
    },
    "resnet50_quantized": {
      "model_dir": "../PPNNI.Mock",
      "model_cmd": "scripts/run-server.sh"
    },
    "sqnet": {
      "model_dir": "../PPNNI.Mock",
      "model_cmd": "scripts/run-server.sh"
    },
    "sqnet_quantized": {
      "model_dir": "../PPNNI.Mock",
      "model_cmd": "scripts/run-server.sh"
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
GET http://192.168.1.249:8080/getServer?model=resnet50&protocol=cheetah
###
```
```json
HTTP/1.1 200 OK
Content-Type: application/json
Connection: close
transfer-encoding: chunked

{
  "ip": "192.168.1.249",
  "port": 9016,
  "model": "resnet50",
  "protocol": "cheetah",
  "status": "ok"
}```

```rest
GET http://192.168.1.249:8080/status
###
```
```json
[HTTP/1.1 200 OK
Content-Type: application/json
Connection: close
transfer-encoding: chunked

[
  {
    "port": 9015,
    "running": true,
    "model": "sqnet_quantized",
    "protocol": "SCI_HE",
    "startedAt": "2025-11-24T21:35:49.408375Z"
  },
  {
    "port": 9016,
    "running": true,
    "model": "resnet50",
    "protocol": "cheetah",
    "startedAt": "2025-11-24T21:35:07.268077Z"
  }
]
```

## 📚 Dependencies

- Kotlin 1.9+
- Ktor 2.3+
- Jackson Kotlin Module
- kotlinx.coroutines 

- ![Server Manager (swagger api)](/media/swagger1.png)

- ![Server Manager (swagger api)](/media/swagger2.png)

