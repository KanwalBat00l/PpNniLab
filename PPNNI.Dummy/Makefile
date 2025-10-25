# ===========================================
# Privacy-Preserving Neural Network Inference
# Multi-target Build System
# ===========================================

# ---------- Directories ----------
BUILD_DIR=build
SRC_DIR=.
BIN_DIR=$(BUILD_DIR)

# Create build directory if missing
$(shell mkdir -p $(BUILD_DIR))

# ---------- macOS settings ----------
OPENSSL_INC=/opt/homebrew/opt/openssl@3/include
OPENSSL_LIB=/opt/homebrew/opt/openssl@3/lib
MAC_FLAGS=-std=c++20 -arch arm64 -I$(OPENSSL_INC) -L$(OPENSSL_LIB) -lssl -lcrypto

# ---------- Android NDK settings ----------
NDK_HOME ?= /Users/$(USER)/Library/Android/sdk/ndk/26.1.10909125
ANDROID_API ?= 21
ANDROID_TRIPLE ?= aarch64-linux-android
ANDROID_TOOLCHAIN=$(NDK_HOME)/toolchains/llvm/prebuilt/darwin-x86_64
ANDROID_CXX=$(ANDROID_TOOLCHAIN)/bin/$(ANDROID_TRIPLE)$(ANDROID_API)-clang++

# If you have OpenSSL cross-compiled for Android
OPENSSL_ANDROID ?= $(HOME)/android-openssl
ANDROID_FLAGS=-std=c++20 -fPIC -shared \
  -I$(OPENSSL_ANDROID)/include \
  -L$(OPENSSL_ANDROID)/lib \
  -lssl -lcrypto

# ---------- Targets ----------
all: $(BIN_DIR)/server $(BIN_DIR)/client $(BIN_DIR)/libclient_android.so

# macOS server binary
$(BIN_DIR)/server: $(SRC_DIR)/server.cpp
	@echo "⚙️  Building server (macOS)..."
	clang++ $(MAC_FLAGS) -o $@ $<
	@echo "✅ Built: $@"

# macOS client binary
$(BIN_DIR)/client: $(SRC_DIR)/client.cpp
	@echo "⚙️  Building client (macOS)..."
	clang++ $(MAC_FLAGS) -o $@ $<
	@echo "✅ Built: $@"

# Android JNI shared library
$(BIN_DIR)/libclient_android.so: $(SRC_DIR)/client.cpp
	@echo "⚙️  Building Android shared library (libclient_android.so)..."
	$(ANDROID_CXX) $(ANDROID_FLAGS) -o $@ $<
	@echo "✅ Built: $@"

# Clean up build folder
clean:
	rm -rf $(BUILD_DIR)
	@echo "🧹 Cleaned all build artifacts."

.PHONY: all clean
