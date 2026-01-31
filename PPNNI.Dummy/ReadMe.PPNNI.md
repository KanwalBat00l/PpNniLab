# Dummy: 🔐 Privacy-Preserving Neural Network Inference (PPNNI)
> This is a dummy implementation of ppnni (the acutal implementation may contain libraries we need to find way to redistribute)

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
ppnni/
├── server.cpp
├── client.cpp
├── build/
│   ├── server
│   ├── client
│   └── libclient_android.so
├── server.crt
├── server.key
├── test.jpg
├── README.PPNNI.md
└── MakeFile (optional)
</pre>

## 🧩 Dependencies
**macOS (Host) 🖥️ **
- Install OpenSSL 3 via Homebrew: `brew install openssl@3`
- Add it to your compiler paths:
```bash
export OPENSSL_ROOT_DIR="/opt/homebrew/opt/openssl@3"
export LDFLAGS="-L$OPENSSL_ROOT_DIR/lib"
export CPPFLAGS="-I$OPENSSL_ROOT_DIR/include"
```

## ⚙️ Compilation
### 🖥️ Compile for macOS (Server & Client)
- Use clang++ (default on macOS):
```bash
<<<<<<< Updated upstream:PPNNI.Dummy/ReadMe.PPNNI.md
# Compile server
clang++ -std=c++20 -arch arm64 server.cpp -o server \
  -I/opt/homebrew/opt/openssl@3/include \
  -L/opt/homebrew/opt/openssl@3/lib \
  -lssl -lcrypto

# Compile client
clang++ -std=c++20 -arch arm64 client.cpp -o client \
  -I/opt/homebrew/opt/openssl@3/include \
  -L/opt/homebrew/opt/openssl@3/lib \
  -lssl -lcrypto
=======
bash scripts/build_mock.sh
>>>>>>> Stashed changes:PPNNI.Mock/ReadMe.PPNNI.md
```

✅ Explanation

- -std=c++20: Enables modern C++ features.
- -arch arm64: Builds for Apple Silicon (M1/M2/M3/M4 Macs).
- -I and -L: Include and link against OpenSSL 3 headers and libs.
- -lssl -lcrypto: Links OpenSSL libraries.

### 🤖 Compile for Android (JNI Shared Library)
- To integrate the client with Android (via JNI):
```bash
bash scripts/build_client_libs.sh
```

## 🚀 Running the System 🖥️ 
- **Step 1: Start the Server**  using `bash scripts/run-server.sh [cheetah|SCI_HE] [resnet50|sqnet|resnet50_quantized|sqnet_quantized] <port>`
- **Step 2: Run the Client** using `bash scripts/run-client.sh [cheetah|SCI_HE] [resnet50|sqnet|resnet50_quantized|sqnet_quantized] <server_ip> <port>`


### Sample Runs
- ![PPNNI (Server and Client)](/media/ppnni.png)
