#!/usr/bin/env bash
# Build mock binaries and copy to OpenCheetah-style names

set -e

BUILD_DIR=build/bin
mkdir -p $BUILD_DIR

echo "[Build] Compiling core binaries..."
clang++ -std=c++20 -O2 mock_server.cpp -o build/mock_server
clang++ -std=c++20 -O2 mock_client.cpp -o build/mock_client

MODELS=("resnet50" "resnet50_quantized" "sqnet" "sqnet_quantized")
MODES=("cheetah" "SCI_HE")

echo "[Build] Copying to OpenCheetah-style names..."
for model in "${MODELS[@]}"; do
    for mode in "${MODES[@]}"; do
        cp build/mock_server "$BUILD_DIR/${model}-${mode}"
        cp build/mock_client "$BUILD_DIR/${model}-${mode}_client"
        echo " → ${model}-${mode}  and  ${model}-${mode}_client"
    done
done

echo "[Build] Building shared library for Android..."
clang++ -std=c++20 -O2 -fPIC -shared mock_client.cpp -o $BUILD_DIR/libmock_client.so
echo " → libmock_client.so built successfully."