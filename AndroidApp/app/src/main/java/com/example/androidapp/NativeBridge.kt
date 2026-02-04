package com.example.androidapp

object NativeBridge {

    init {
        // Matches the name in CMakeLists.txt
        System.loadLibrary("client_jni")
    }

    /**
     * Native call to the compiled C++ mock_client.cpp logic.
     */
    external fun runMockClient(
        protocol: String,
        model: String,
        ip: String,
        port: Int,
        filesDir: String
    ): String
}