package com.example.androidapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

/**
 * ImagePreprocessor:
 * Converts an image URI into a fixed-point .inp file compatible with native clients.
 */
object ImagePreprocessor {

    /**
     * Converts an image to normalized fixed-point input format (.inp).
     *
     * @param context Application context
     * @param imageUri Uri of the image selected by the user
     * @param model Model name (for naming the output file)
     * @param protocol Protocol name (for naming the output file)
     * @param scale Scaling factor (used for fixed-point quantization)
     * @return File path to the generated .inp file
     */
    fun convertToInpFile(
        context: Context,
        imageUri: Uri,
        model: String,
        protocol: String,
        scale: Int = 12
    ): File {
        val bmp = context.contentResolver.openInputStream(imageUri)?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: throw Exception("Failed to decode image")

        // Resize to 320x320 (mock preprocessing)
        val targetSize = 320
        val resized = Bitmap.createScaledBitmap(bmp, targetSize, targetSize, true)

        val w = resized.width
        val h = resized.height
        val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
        val std = floatArrayOf(0.229f, 0.224f, 0.225f)

        val values = Array(3) { FloatArray(w * h) }
        val pixels = IntArray(w * h)
        resized.getPixels(pixels, 0, w, 0, 0, w, h)

        for (y in 0 until h) {
            for (x in 0 until w) {
                val px = pixels[y * w + x]
                val r = ((px shr 16) and 0xff) / 255.0f
                val g = ((px shr 8) and 0xff) / 255.0f
                val b = (px and 0xff) / 255.0f
                val idx = y * w + x
                values[0][idx] = (r - mean[0]) / std[0]
                values[1][idx] = (g - mean[1]) / std[1]
                values[2][idx] = (b - mean[2]) / std[2]
            }
        }

        val outFile = File(context.cacheDir, "${model}_${protocol}_input_fixedpt_scale_$scale.inp")
        outFile.bufferedWriter().use { writer ->
            for (c in 0..2) {
                val arr = values[c]
                for (i in arr.indices) {
                    val scaled = (arr[i] * (1 shl scale)).toLong()
                    writer.write(scaled.toString())
                    writer.write(" ")  // Explicit string write (fixes error)
                }
            }
            writer.write("\n")
        }

        return outFile
    }
}
