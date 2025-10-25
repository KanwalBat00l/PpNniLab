package com.example.androidapp

import java.io.File

/**
 * NativeBridge:
 * Lightweight JNI wrapper to call the native C++ client library.
 * This design keeps native logic decoupled from Android components,
 * improving maintainability and cross-platform portability.
 */
object NativeBridge {
    init {
        System.loadLibrary("client_jni")
    }

    /** Direct JNI call mapping */
    external fun runClient(ip: String, port: Int, imagePath: String): String

    /** Helper for Kotlin: accepts a File instead of raw path */
    fun execute(ip: String, port: Int, file: File): String {
        return runClient(ip, port, file.absolutePath)
    }
}
