# PPNNI.Mock — Privacy-Preserving Neural Network Inference Simulator
## 🌐 Overview

**PPNNI.Mock** is a high-fidelity simulation of the **SecFePAS** (Secure Facial-expression-based Pain Assessment) system. Due to IP and privacy restrictions regarding the OpenCheetah library and the PAIN dataset, this mock implementation was developed to provide a distributable, verifiable demonstration of the system's architecture, data flow, and secure protocol orchestration.

> This system mimics runtime behavior, including:
- Simulated TLS Handshake: Demonstration of "Security by Design."
- Protocol Negotiation: Server-side validation of model and protocol types.
- Deterministic Inference: Mock computation using model weights and client input files.
- Performance Scaling: Simulated computational overhead differences between OpenCheetah and SCI_HE, and between Full and Quantized models.

.
📂 Directory Layout
<Text>
PPNNI.Mock/
├── mock_common.hpp      # Shared logic, networking helpers, and configuration
├── mock_server.cpp      # Server-side binary source (The Orchestrated Model)
├── mock_client.cpp      # Client-side binary source (JNI-compatible core)
├── Makefile             # Automated build system with Catch2 integration
├── pretrained/          # Mock weights and input shares (.inp files)
│   ├── resnet50_mock_weights.inp
│   ├── sqnet_mock_weights.inp
│   └── mock_input.inp
├── scripts/             # Orchestration and verification scripts
│   ├── run-server.sh    # Script used by Kotlin Server Manager
│   ├── run-client.sh    # Script for local client execution
│   └── verify_matrix.sh # Automated 8-path combinatorial test
└── tests/               # Professional Unit Test Suite
    ├── test_main.cpp    # Catch2 test cases
    └── catch.hpp        # Catch2 Unit Testing Framework (v2.x)
</Text>



## 📂 Directory Layout
<pre>
PPNNI.Mock/
├── mock_client.cpp
├── mock_server.cpp
├── mock_common.hpp
├── pretrained/
│   ├── resnet50_mock_weights.inp
│   ├── resnet50_quantized_mock_weights.inp
│   ├── sqnet_mock_weights.inp
│   ├── sqnet_mock_quantized_weights.inp
│   └── mock_input.inp --> test input
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

## 🛠️ Dependencies
-  C++20 Compiler: (clang++ or g++)
-  Standard Libraries: BSD/Linux sockets (sys/socket.h, netinet/in.h)
-  Python 3.8+: Required only for the verify_matrix.sh timing logic.
-  Curl: Used by Makefile to automatically fetch the testing framework.

## ⚙️ Compilation
### 🖥️ Host (macOS / Linux)
The system uses a centralized Makefile to ensure all components are built with consistent flags.

```bash
# Build production binaries and the unit test suite
make all

# Clean build artifacts
make clean
```
- Use clang++ (default on macOS):

### 🤖 Compile for Android (JNI Shared Library)
- To integrate the client with Android (via JNI):
```bash
# Example for NDK cross-compilation
$NDK_HOME/toolchains/llvm/prebuilt/darwin-x86_64/bin/aarch64-linux-android21-clang++ \
  -std=c++20 -fPIC -shared mock_client.cpp -o libclient_jni.so -DANDROID_JNI
```




## 🚀 Running the System 🖥️ 
- **Step 1: Start the Server** on some port 
```bash
# Usage: ./build/mock_server [cheetah|SCI_HE] [resnet50|sqnet] [port]
./build/mock_server cheetah resnet50 8000
```

- **Step 2: Run the Client** Provide the server’s IP, port, and an input image file as .inp: 
```bash
# Usage: ./build/mock_client [protocol] [model] [ip] [port] [input_file]
./build/mock_client cheetah resnet50 127.0.0.1 8000 pretrained/mock_input.inp
```
- **Testing & Verification**
```bash
# Compile and run verbose unit tests
make test
./build/unit_test --success --durations yes
bash scripts/verify_matrix.sh
```

- Note: This implementation is for verification and architectural demonstration purposes only. It does not provide real cryptographic security.
- The PPNNI.Mock system implements a Simulated TLS layer to demonstrate the secure handshake sequence required by the architecture without necessitating external OpenSSL dependencies for the demonstration environment.