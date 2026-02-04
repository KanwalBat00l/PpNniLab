package com.example.androidapp

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class ServerResponse(
    val ip: String,
    val port: Int,
    val model: String,
    val protocol: String,
    val status: String
)

class ClientManager(private val context: Context) {

    private val client = OkHttpClient()

    /**
     * Queries the Server Manager to start a C++ mock instance and return its connection info.
     */
    suspend fun requestServer(
        baseUrl: String,
        model: String,
        protocol: String
    ): Result<ServerResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/getServer?model=$model&protocol=$protocol"
            val response = client.newCall(Request.Builder().url(url).build()).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}"))
            }

            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
            val json = JSONObject(body)

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

    /**
     * Executes the C++ client via JNI.
     * Automatically passes the internal filesDir path to the native layer.
     */
    suspend fun runMockClient(
        protocol: String,
        model: String,
        serverIp: String,
        serverPort: Int
    ): String = withContext(Dispatchers.IO) {
        return@withContext try {
            // We get the absolute path of filesDir so the C++ code knows where to read .inp files
            val internalPath = context.filesDir.absolutePath

            val result = NativeBridge.runMockClient(
                protocol,
                model,
                serverIp,
                serverPort,
                internalPath
            )

            Log.i("ClientManager", "Native output: $result")
            result
        } catch (e: Exception) {
            Log.e("ClientManager", "JNI Execution failed", e)
            "Error: ${e.message}"
        }
    }

    /**
     * Parses the numeric shares returned by the C++ code to determine the result.
     */
    fun parseClientOutput(output: String): String {
        val lines = output.lines()
        // Extract lines that look like numbers
        val numericLines = lines
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.all { char -> char.isDigit() } }
            .map { it.toInt() }

        if (numericLines.isEmpty()) return "Inference completed (No numeric shares found)"

        // Standard ArgMax to find predicted class
        val predictedClass = numericLines.indices.maxByOrNull { numericLines[it] } ?: -1
        return "Pain Class: $predictedClass"
    }
}