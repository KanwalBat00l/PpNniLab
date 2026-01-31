package com.example.server.service

import com.example.server.config.Config
import kotlinx.coroutines.*
import java.io.File
import java.net.ServerSocket
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class ServerService(private val config: Config) {

    private val activeServers = ConcurrentHashMap<Int, Process>()
    private val serverMetadata = ConcurrentHashMap<Int, ServerInstance>()

    data class ServerInstance(
        val model: String,
        val protocol: String,
        val startTime: Instant
    )

    fun startServer(model: String, protocol: String): Map<String, Any> {
        // 1. Validation
        val validProtocols = listOf("cheetah", "SCI_HE")
        if (protocol !in validProtocols) throw IllegalArgumentException("Invalid protocol: $protocol")
        if (!config.models.containsKey(model)) throw IllegalStateException("Model $model not configured")

        // 2. Port Allocation
        val port = getFreePort()

        // 3. Process Execution
        val modelCfg = config.models[model]!!
        val workingDir = File(modelCfg.model_dir).canonicalFile
        val script = File(workingDir, modelCfg.model_cmd).canonicalFile

        if (!script.exists()) throw IllegalStateException("Binary not found at ${script.absolutePath}")

        val pb = ProcessBuilder("bash", script.absolutePath, protocol, model, port.toString())
            .directory(workingDir)
            .redirectErrorStream(true)

        val process = pb.start()

        // 4. Tracking
        activeServers[port] = process
        serverMetadata[port] = ServerInstance(model, protocol, Instant.now())

        // 5. Lifecycle Management (Async)
        CoroutineScope(Dispatchers.IO).launch {
            launch {
                process.inputStream.bufferedReader().forEachLine { line ->
                    println("[Server:$port] $line")
                }
            }

            val exited = process.waitFor(config.serverLifetimeMs, TimeUnit.MILLISECONDS)
            if (!exited) {
                println("[Manager] Port $port timed out. Killing process.")
                process.destroyForcibly()
            }
            cleanup(port)
        }

        return mapOf(
            "ip" to config.hostIp,
            "port" to port,
            "model" to model,
            "protocol" to protocol,
            "status" to "ok"
        )
    }

    private fun getFreePort(): Int {
        val start = config.portRange.first()
        val end = config.portRange.last()
        val ports = (start..end).shuffled()
        
        return ports.firstOrNull { port ->
            !activeServers.containsKey(port) && isPortAvailable(port)
        } ?: throw IllegalStateException("No free ports available in range $start-$end")
    }

    private fun isPortAvailable(port: Int): Boolean = try {
        ServerSocket(port).use { true }
    } catch (e: Exception) {
        false
    }

    fun getStatus(): List<Map<String, Any?>> = serverMetadata.map { (port, meta) ->
        mapOf(
            "port" to port,
            "running" to true,
            "model" to meta.model,
            "protocol" to meta.protocol,
            "startedAt" to meta.startTime.toString()
        )
    }

    private fun cleanup(port: Int) {
        activeServers.remove(port)
        serverMetadata.remove(port)
    }
}