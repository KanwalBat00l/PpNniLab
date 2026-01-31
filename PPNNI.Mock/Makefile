# Compiler and Flags
CXX = g++
CXXFLAGS = -std=c++20 -O2 -Wall
BUILD_DIR = build
TEST_DIR = tests

# Catch2 Download URL (v2.13.10 is the stable single-header version)
CATCH_URL = https://raw.githubusercontent.com/catchorg/Catch2/v2.x/single_include/catch2/catch.hpp

# Standard targets
.PHONY: all clean test prep download_catch

# 1. 'all' builds both production and test binaries
all: prep download_catch $(BUILD_DIR)/mock_server $(BUILD_DIR)/mock_client $(BUILD_DIR)/unit_test

# Create build directory
prep:
	@mkdir -p $(BUILD_DIR)
	@mkdir -p $(TEST_DIR)

# Automatically download Catch2 if missing (Verifiers love this automation)
download_catch:
	@if [ ! -f $(TEST_DIR)/catch.hpp ]; then \
		echo "Downloading Catch2 header..."; \
		curl -sL $(CATCH_URL) -o $(TEST_DIR)/catch.hpp; \
	fi

# Production: Server
$(BUILD_DIR)/mock_server: mock_server.cpp mock_common.hpp
	$(CXX) $(CXXFLAGS) mock_server.cpp -o $(BUILD_DIR)/mock_server

# Production: Client
$(BUILD_DIR)/mock_client: mock_client.cpp mock_common.hpp
	$(CXX) $(CXXFLAGS) mock_client.cpp -o $(BUILD_DIR)/mock_client

# Test: Unit Test Runner
$(BUILD_DIR)/unit_test: $(TEST_DIR)/test_main.cpp mock_common.hpp
	$(CXX) $(CXXFLAGS) $(TEST_DIR)/test_main.cpp -o $(BUILD_DIR)/unit_test

# 2. 'test' runs the compiled unit tests with professional output
test: all
	@echo "\n>>> Running Professional Unit Test Suite <<<"
	./$(BUILD_DIR)/unit_test --success --durations yes

# Cleanup
clean:
	rm -rf $(BUILD_DIR)
	@echo "Build directory cleaned."