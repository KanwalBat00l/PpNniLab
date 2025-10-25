package com.example.androidapp

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File

/**
 * Handles all non-UI logic:
 * - Communicates with backend server via HTTP.
 * - Prepares image data for native layer.
 * - Invokes native client through JNI.
 */
data class ServerResponse(val ip: String, val port: Int, val model: String, val status: String)

class ClientManager(private val context: Context) {

    private val client = OkHttpClient()

    /**
     * Requests an available secure server node for a given model.
     * @param baseUrl Base URL of the backend API (e.g., http://192.168.1.249:8080)
     * @param model Model name (optional)
     */
    suspend fun requestServer(baseUrl: String, model: String = "default"): Result<ServerResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(
                    Request.Builder()
                        .url("$baseUrl/getServer?model=$model")
                        .build()
                ).execute()

                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }

                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                val json = JSONObject(body)

                Result.success(
                    ServerResponse(
                        ip = json.getString("ip"),
                        port = json.getInt("port"),
                        model = json.optString("model", model),
                        status = json.optString("status", "ok")
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Runs the secure inference native client via JNI bridge.
     * @param ip IP address of the server to connect to.
     * @param port Port of the running inference service.
     * @param imageUri Image selected by user.
     */
    suspend fun runNativeClient(ip: String, port: Int, imageUri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val file = File(context.cacheDir, "selected_image.jpg")
                context.contentResolver.openInputStream(imageUri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }

                val output = NativeBridge.execute(ip, port, file)
                Result.success(output)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
