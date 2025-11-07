CXX = g++
CXXFLAGS = -std=c++20 -O2
BUILD_DIR = build

all: mock_client mock_server

mock_client: mock_client.cpp
	mkdir -p $(BUILD_DIR)
	$(CXX) $(CXXFLAGS) mock_client.cpp -o $(BUILD_DIR)/mock_client

mock_server: mock_server.cpp
	mkdir -p $(BUILD_DIR)
	$(CXX) $(CXXFLAGS) mock_server.cpp -o $(BUILD_DIR)/mock_server

clean:
	rm -rf $(BUILD_DIR)/*
