# PPNNI.Mock — Simulated Privacy-Preserving Neural Network Inference
> This branch provides a mock implementation of PPNNI that simulates the client-server execution flow without using real cryptographic protocols. It is designed to mimic runtime behavior, data flow, and logging, allowing easy testing and integration without secure computation dependencies.

##  🧱 Components
**File	Description**
- **server.cpp**	Listens on a port, performs a TLS handshake, receives an encrypted data share, computes partial results, and sends them back.
- **client.cpp**	Connects to the server, performs handshake, encrypts and sends a data share, then computes final result locally.
- *libclient_android.so*	JNI-compatible client binary compiled for Android (ARM64).
- *client and server*	client and server binary compiled for host.
- *Makefile*: instruction to make file
- *server certificate files*
- test image: hs0.png
- ReadMe.PPNNI.md this information file

## 📂 Directory Layout
<pre>
PPNNI.Mock/
├── mock_client.cpp
├── mock_server.cpp
├── mock_common.hpp
├── pretrained/
│   ├── resnet50_mock_input.inp
│   ├── resnet50_mock_weights.inp
│   ├── sqnet_mock_input.inp
│   └── sqnet_mock_weights.inp
├── scripts/
│   ├── run-client.sh
│   ├── run-server.sh
│   └── common.sh
├── build/
│   ├── mock_client
│   └── mock_server
├── Makefile
└── README.MOCK.md

</pre>

## 🧩 Dependencies
**macOS (Host) 🖥️ **
- C++20 compiler (clang++ or g++)
- Python 3.8+ (for input preprocessing)
- Optional: matplotlib, numpy (for preprocess.py visualization


## ⚙️ Compilation
### 🖥️ Compile for macOS (Server & Client)
```bash
# Compile server
clang++ -std=c++20 -O2 -o build/mock_server mock_server.cpp

# Compile client
clang++ -std=c++20 -O2 -o build/mock_client mock_client.cpp

```
- Use clang++ (default on macOS):

### 🤖 Compile for Android (JNI Shared Library)
- To integrate the client with Android (via JNI):
```bash
# Example for NDK cross-compilation
$NDK_HOME/toolchains/llvm/prebuilt/darwin-x86_64/bin/aarch64-linux-android21-clang++ \
  -std=c++20 -fPIC -shared client.cpp -o libclient_android.so \
  -I$OPENSSL_ANDROID/include \
  -L$OPENSSL_ANDROID/lib \
  -lssl -lcrypto
```

✅ Flags explained

- -shared: Build as a .so shared library.
- -fPIC: Generate position-independent code for dynamic linking.
- aarch64-linux-android21-clang++: NDK cross-compiler for Android ARM64 API level 21+.
- Make sure $NDK_HOME and $OPENSSL_ANDROID point to your NDK and OpenSSL build for Android.

## 🔐 TLS Certificate Setup

- Before running, create a self-signed TLS certificate for the server. Both must be in the same directory as server.
```bash
openssl req -x509 -newkey rsa:2048 \
  -keyout server.key -out server.crt \
  -days 365 -nodes -subj "/CN=localhost"
```
- will generate
- server.crt   ← Certificate
- server.key   ← Private key

## 🚀 Running the System 🖥️ 
- **Step 1: Start the Server** on some port `./server 6000`

- **Step 2: Run the Client** Provide the server’s IP, port, and an input image file: `./client 127.0.0.1 6000 test.jpg`
