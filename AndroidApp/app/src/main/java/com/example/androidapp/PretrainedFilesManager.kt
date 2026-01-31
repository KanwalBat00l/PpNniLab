package com.example.androidapp

import android.content.Context
import android.util.Log
import java.io.File

object PretrainedFilesManager {

    private const val TAG = "PretrainedFiles"

    fun copyPretrainedFiles(context: Context) {
        try {
            val assetManager = context.assets
            val assetFiles = assetManager.list("pretrained") ?: arrayOf()
            if (assetFiles.isEmpty()) {
                Log.e(TAG, "No pretrained files found in assets/pretrained")
                return
            }

            // Destination: CWD for JNI client (relative path 'pretrained/')
            val cwdDir = File(context.filesDir.parentFile, "pretrained")
            if (!cwdDir.exists()) {
                cwdDir.mkdirs()
                Log.i(TAG, "Created directory: ${cwdDir.absolutePath}")
            }

            for (fileName in assetFiles) {
                val outFile = File(cwdDir, fileName)
                if (!outFile.exists()) {
                    assetManager.open("pretrained/$fileName").use { input ->
                        outFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    Log.i(TAG, "Copied file to: ${outFile.absolutePath}")
                } else {
                    Log.i(TAG, "File already exists: ${outFile.absolutePath}")
                }
            }

            // Also log CWD just to verify
            Log.i(TAG, "CWD for JNI client: ${cwdDir.absolutePath}")
            Log.i(TAG, "Files in directory: ${cwdDir.listFiles()?.joinToString { it.name } ?: "none"}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy pretrained files: ${e.message}", e)
        }
    }
}
