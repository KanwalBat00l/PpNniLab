package com.example.server.service

import com.example.server.config.Config
import com.example.server.config.ModelConfig
import kotlinx.coroutines.*
import java.io.File
import java.net.ServerSocket
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class ServerService(private val config: Config) {

    private val active = ConcurrentHashMap<Int, Process>()
    private val startTime = ConcurrentHashMap<Int, Instant>()
    private val modelByPort = ConcurrentHashMap<Int, String>()
    private val protocolByPort = ConcurrentHashMap<Int, String>()

    private val portPool: List<Int> by lazy {
        val start = config.portRange[0]
        val end = config.portRange[1]
        (start..end).toList()
    }

    /** Spawns a server using bash script with protocol, model, and port */
    private fun spawnServer(port: Int, model: String, protocol: String): Process {
        val modelCfg: ModelConfig = config.models[model]
            ?: throw IllegalStateException("No configuration for model '$model'")
        val modelDir = File(modelCfg.model_dir)
        require(modelDir.exists()) { "Model directory not found: ${modelDir.absolutePath}" }

        println("▶️ Launching ${modelCfg.model_cmd} for $protocol $model on port $port")

        // Example: bash scripts/run-server.sh cheetah resnet50 6000
        return ProcessBuilder(modelCfg.model_cmd, protocol, model, port.toString())
            .directory(modelDir)
            .redirectErrorStream(true)
            .start()
    }

    /** Checks if a port is available at the OS level */
    private fun isPortAvailable(port: Int): Boolean {
        return try {
            ServerSocket(port).close()
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Allocates a random free port and starts a server */
    fun startServer(model: String, protocol: String = "cheetah"): Map<String, Any> {
        val shuffledPorts = portPool.shuffled(Random(System.currentTimeMillis()))
        val freePort = shuffledPorts.firstOrNull { 
            !active.containsKey(it) && isPortAvailable(it) 
        } ?: throw IllegalStateException("No free port available")

        val proc = spawnServer(freePort, model, protocol)
        active[freePort] = proc
        startTime[freePort] = Instant.now()
        modelByPort[freePort] = model
        protocolByPort[freePort] = protocol

        val scope = CoroutineScope(Dispatchers.IO)

        // Capture server logs asynchronously
        scope.launch {
            proc.inputStream.bufferedReader().forEachLine { line ->
                println("[server:$freePort][$model][$protocol] $line")
            }
        }

        // Monitor server lifetime asynchronously
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
            "protocol" to protocol,
            "status" to "ok"
        )
    }

    /** Returns statuses of currently running servers only */
    fun getStatus(): List<Map<String, Any?>> =
        active.keys.map { port ->
            mapOf(
                "port" to port,
                "running" to true,
                "model" to modelByPort[port],
                "protocol" to protocolByPort[port],
                "startedAt" to startTime[port]?.toString()
            )
        }

    /** Cleanup after process exit */
    private fun cleanup(port: Int) {
        active.remove(port)
        startTime.remove(port)
        modelByPort.remove(port)
        protocolByPort.remove(port)
    }
}
