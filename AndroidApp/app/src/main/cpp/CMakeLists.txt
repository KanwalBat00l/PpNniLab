cmake_minimum_required(VERSION 3.22.1)
project("androidapp")

# 1. Define the path to the Mock source (5 levels up)
set(MOCK_SRC_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../../PPNNI.Mock")

# 2. Tell CMake where to find mock_common.hpp
include_directories(${MOCK_SRC_DIR})

# 3. Build the library
add_library(client_jni SHARED
        client_jni.cpp
        ${MOCK_SRC_DIR}/mock_client.cpp
)

# 4. Link Android logs
target_link_libraries(client_jni log)

# 5. Set C++20 standard
set_target_properties(client_jni PROPERTIES CXX_STANDARD 20)