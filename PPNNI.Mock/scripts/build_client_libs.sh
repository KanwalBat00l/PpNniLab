#!/usr/bin/env bash
set -e

NDK_PATH="/Users/kanwalbatool/Library/Android/sdk/ndk/29.0.14206865"
BUILD_DIR="build/android"
mkdir -p $BUILD_DIR

ABI="arm64-v8a"
API_LEVEL=21
CXX="$NDK_PATH/toolchains/llvm/prebuilt/darwin-x86_64/bin/aarch64-linux-android${API_LEVEL}-clang++"
CXXFLAGS="-std=c++20 -O2 -fPIC"

OUT="$BUILD_DIR/libmock_client.so"
echo "[Build] Compiling $OUT"
$CXX $CXXFLAGS -shared mock_client.cpp -o "$OUT"

echo "[Build] Mock client built: $OUT"
