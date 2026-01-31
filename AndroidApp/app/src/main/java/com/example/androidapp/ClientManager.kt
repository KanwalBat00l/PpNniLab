package com.example.androidapp

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import android.util.Log

data class ServerResponse(
    val ip: String,
    val port: Int,
    val model: String,
    val protocol: String,
    val status: String
)

class ClientManager(private val context: Context) {

    private val client = OkHttpClient()

    // --- Step 1: Query remote server for IP/port ---
    suspend fun requestServer(
        baseUrl: String,
        model: String,
        protocol: String
    ): Result<ServerResponse> = withContext(Dispatchers.IO) {
        try {
            val response = client.newCall(
                Request.Builder()
                    .url("$baseUrl/getServer?model=$model&protocol=$protocol")
                    .build()
            ).execute()

            if (!response.isSuccessful)
                return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))

            val bodyString = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            val json = JSONObject(bodyString)

            Result.success(
                ServerResponse(
                    ip = json.getString("ip"),
                    port = json.getInt("port"),
                    model = json.getString("model"),
                    protocol = json.getString("protocol"),
                    status = json.optString("status", "ok")
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Step 2: Run JNI client ---
    suspend fun runMockClient(
        protocol: String,   // cheetah | SCI_HE
        model: String,      // resnet50 | sqnet etc.
        serverIp: String,
        serverPort: Int
    ): String {
        return try {
            val result = NativeBridge.runMockClient(protocol, model, serverIp, serverPort)
            Log.i("ClientManager", "Client output: $result")
            result
        } catch (e: Exception) {
            Log.e("ClientManager", "Failed to run mock client", e)
            "Error: ${e.message}"
        }
    }


    fun parseClientOutput(output: String): String {
        // Split output into lines
        val lines = output.lines()

        // Take only the numeric lines at the end
        val numericLines = lines
            .filter { it.trim().matches(Regex("\\d+")) }
            .map { it.trim().toInt() }

        if (numericLines.isEmpty()) return "No prediction"

        // Argmax: find index of max value
        val predictedClass = numericLines.indices.maxByOrNull { numericLines[it] } ?: -1

        return "Pain class $predictedClass"
    }


}
