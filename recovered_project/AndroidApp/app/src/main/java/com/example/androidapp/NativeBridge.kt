package com.example.androidapp

object NativeBridge {

    // Load the JNI wrapper library (not the prebuilt client)
    init {
        System.loadLibrary("client_jni")
    }

    // JNI function implemented in client_jni.cpp
    external fun runMockClient(
        protocol: String,   // cheetah | SCI_HE
        model: String,      // resnet50 | sqnet | resnet50_quantized | sqnet_quantized
        ip: String,         // server IP
        port: Int           // server port
    ): String
}
