package com.example.server.service

import com.example.server.config.Config
import com.example.server.config.ModelConfig
import kotlinx.coroutines.*
import java.io.File
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/** Handles business logic: process management and model allocation */
class ServerService(private val config: Config) {

    private val active = ConcurrentHashMap<Int, Process>()
    private val startTime = ConcurrentHashMap<Int, Instant>()
    private val modelByPort = ConcurrentHashMap<Int, String>()

    /** Spawns a model server as a subprocess */
    private fun spawnServer(port: Int, model: String): Process {
        val modelCfg: ModelConfig = config.models[model] ?: config.models["default"]
            ?: throw IllegalStateException("❌ No configuration for model '$model'")

        val modelDir = File(modelCfg.model_dir)
        require(modelDir.exists()) { "Model directory not found: ${modelDir.absolutePath}" }

        println("▶️ Launching ${modelCfg.model_cmd} in ${modelCfg.model_dir} on port $port")

        return ProcessBuilder(modelCfg.model_cmd, port.toString())
            .directory(modelDir)
            .redirectErrorStream(true)
            .start()
    }

    /** Allocates a free port and starts a model server */
    fun startServer(model: String): Map<String, Any> {
        val freePort = config.portPool.firstOrNull { !active.containsKey(it) }
            ?: throw IllegalStateException("No free port available")

        val proc = spawnServer(freePort, model)
        active[freePort] = proc
        startTime[freePort] = Instant.now()
        modelByPort[freePort] = model

        val scope = CoroutineScope(Dispatchers.IO)

        // Capture logs asynchronously
        scope.launch {
            proc.inputStream.bufferedReader().forEachLine {
                println("[server:$freePort][$model] $it")
            }
        }

        // Manage lifetime
        scope.launch {
            val finished = proc.waitFor(config.serverLifetimeMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            if (!finished) {
                println("[manager] Server on $freePort timed out, killing.")
                proc.destroyForcibly()
            } else {
                println("[manager] Server on $freePort exited with code ${proc.exitValue()}")
            }
            cleanup(freePort)
        }

        return mapOf(
            "ip" to config.hostIp,
            "port" to freePort,
            "model" to model,
            "status" to "ok"
        )
    }

    /** Returns server statuses */
    fun getStatus(): List<Map<String, Any?>> =
        config.portPool.map {
            mapOf(
                "port" to it,
                "running" to active.containsKey(it),
                "model" to modelByPort[it],
                "startedAt" to startTime[it]?.toString()
            )
        }

    /** Cleans up when a process exits */
    private fun cleanup(port: Int) {
        active.remove(port)
        startTime.remove(port)
        modelByPort.remove(port)
    }
}
