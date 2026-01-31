#!/usr/bin/env bash
# Build mock binaries and copy to OpenCheetah-style names

set -e

# Paths
ROOT_DIR="$(dirname "$0")/.."
BUILD_DIR="$ROOT_DIR/build/bin"
CORE_BUILD_DIR="$ROOT_DIR/build"

mkdir -p "$BUILD_DIR"
mkdir -p "$CORE_BUILD_DIR"

echo "[Build] Compiling core binaries..."

# Compile mock server & client once
clang++ -std=c++20 -O2 "$ROOT_DIR/mock_server.cpp" -o "$CORE_BUILD_DIR/mock_server"
clang++ -std=c++20 -O2 "$ROOT_DIR/mock_client.cpp" -o "$CORE_BUILD_DIR/mock_client"

# Ensure executables have correct permission
chmod +x "$CORE_BUILD_DIR/mock_server" "$CORE_BUILD_DIR/mock_client"

MODELS=("resnet50" "resnet50_quantized" "sqnet" "sqnet_quantized")
MODES=("cheetah" "SCI_HE")

echo "[Build] Creating OpenCheetah-style binaries..."
for model in "${MODELS[@]}"; do
    for mode in "${MODES[@]}"; do
        
        server_out="${BUILD_DIR}/${model}-${mode}"
        client_out="${BUILD_DIR}/${model}-${mode}_client"

        cp "$CORE_BUILD_DIR/mock_server" "$server_out"
        cp "$CORE_BUILD_DIR/mock_client" "$client_out"

        chmod +x "$server_out" "$client_out"

        echo " → Built: $(basename "$server_out") and $(basename "$client_out")"
    done
done

echo " → Build successful."
