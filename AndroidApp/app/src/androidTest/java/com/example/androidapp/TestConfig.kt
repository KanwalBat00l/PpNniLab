package com.example.androidapp

/**
 * Global Configuration for Android Instrumented Tests.
 * verifiers can change these values here to match their local environment.
 */
object TestConfig {
    // For Emulator use: "http://10.0.2.2:8080"
    // For Physical Phone use your Mac's IP: "http://10.168.212.227:8080"
    const val MANAGER_URL = "http://10.0.2.2:8080"

    const val DEFAULT_MODEL = "sqnet"
    const val DEFAULT_PROTOCOL = "cheetah"
}