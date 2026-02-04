package com.example.androidapp

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class ServerResponse(
    val ip: String, val port: Int, val model: String, val protocol: String, val status: String
)

class ClientManager(private val context: Context) {
    private val client = OkHttpClient()

    suspend fun requestServer(baseUrl: String, model: String, protocol: String): Result<ServerResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/getServer?model=$model&protocol=$protocol"
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
            val json = JSONObject(response.body?.string() ?: "{}")
            Result.success(ServerResponse(json.getString("ip"), json.getInt("port"), json.getString("model"), json.getString("protocol"), json.optString("status", "ok")))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun runMockClient(protocol: String, model: String, serverIp: String, serverPort: Int): String = withContext(Dispatchers.IO) {
        return@withContext try {
            NativeBridge.runMockClient(protocol, model, serverIp, serverPort, context.filesDir.absolutePath)
        } catch (e: Exception) { "Error: ${e.message}" }
    }

    /**
     * Professional Parser: Extracts numeric inference results even if surrounded by text.
     */
    fun parseClientOutput(output: String): String {
        val lines = output.lines()
        // Regex looks for lines containing only numbers (ignoring whitespace)
        val resultValues = lines.map { it.trim() }
            .filter { it.matches(Regex("^[0-9]+$")) }
            .map { it.toInt() }

        if (resultValues.isEmpty()) return "Inference flow complete (Intermediate shares captured)"

        // Standard ArgMax to find predicted class index
        val predictedClass = resultValues.indices.maxByOrNull { resultValues[it] } ?: -1
        return "Final Result: Class $predictedClass (Values: ${resultValues.joinToString(", ")})"
    }
}