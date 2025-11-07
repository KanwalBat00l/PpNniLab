#!/usr/bin/env bash
# scripts/build-mock.sh
# Build mock binaries and copy to OpenCheetah-style names

set -e

BUILD_DIR=build
BIN_DIR=$BUILD_DIR/bin
mkdir -p $BIN_DIR

echo "Step 1: Compile mock_server and mock_client..."
clang++ -std=c++20 -O2 mock_server.cpp -o $BUILD_DIR/mock_server
clang++ -std=c++20 -O2 mock_client.cpp -o $BUILD_DIR/mock_client

echo "Step 2: Copy to OpenCheetah-style names..."
MODELS=("resnet50" "resnet50_quantized" "sqnet" "sqnet_quantized")
MODES=("cheetah" "SCI_HE")

for model in "${MODELS[@]}"; do
    for mode in "${MODES[@]}"; do
        cp $BUILD_DIR/mock_server $BIN_DIR/${model}-${mode}
        cp $BUILD_DIR/mock_client $BIN_DIR/${model}-${mode}
        echo "Created $BIN_DIR/${model}-${mode}"
    done
done

echo "Step 3: Build shared library for Android (libmock_client.so)..."
clang++ -std=c++20 -O2 -fPIC -shared mock_client.cpp -o $BIN_DIR/libmock_client.so
echo "Created $BIN_DIR/libmock_client.so"

echo "Build finished. All binaries are in $BIN_DIR"
