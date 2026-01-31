package com.example.androidapp

import android.content.Context
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * MockImageInputManager:
 * Handles selecting an image and converting it to a mock .inp file
 * compatible with the JNI client.
 */
object MockImageInputManager {

    /**
     * Processes a selected image: converts it to fixed-point .inp and
     * saves in the pretrained folder using the naming convention:
     *   <model>_mock_input.inp
     */
    fun processImage(
        context: Context,
        imageUri: Uri,
        model: String,
        protocol: String,
        callback: (File) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Convert image to .inp file (temporary cache)
                val inpFile = ImagePreprocessor.convertToInpFile(
                    context = context,
                    imageUri = imageUri,
                    model = "${model}_mock",
                    protocol = protocol
                )

                // Ensure pretrained directory exists
                val pretrainedDir = File(context.filesDir, "pretrained")
                if (!pretrainedDir.exists()) pretrainedDir.mkdirs()

                // Copy to pretrained folder with correct name
                val targetFile = File(pretrainedDir, "${model}_mock_input.inp")
                inpFile.copyTo(targetFile, overwrite = true)

                // Notify caller on main thread
                CoroutineScope(Dispatchers.Main).launch {
                    callback(targetFile)
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Failed to create mock input: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
