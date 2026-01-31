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
    private val protocolByPort = ConcurrentHashMap<Int, String>() // Track protocol per port

    /** Spawns a server using bash script with protocol, model, and port */
    private fun spawnServer(port: Int, model: String, protocol: String): Process {
        val modelCfg = config.models[model]
            ?: throw IllegalStateException("No configuration for model '$model'")

        val modelDir = File(modelCfg.model_dir).canonicalFile
        val scriptFile = File(modelDir, modelCfg.model_cmd).canonicalFile

        require(scriptFile.exists()) { "Script not found: ${scriptFile.absolutePath}" }

        println("▶️ Using modelDir: ${modelDir.absolutePath}")
        println("▶️ Using script:   ${scriptFile.absolutePath}")

        val pb = ProcessBuilder(
            "bash",
            scriptFile.absolutePath,
            protocol,
            model,
            port.toString()
        )

        pb.environment().clear()
        pb.environment().putAll(System.getenv())
        pb.directory(modelDir)
        pb.redirectErrorStream(true)

        return pb.start()
    }

    /** Returns a free port randomly from the port range */
    private fun getFreePort(): Int {
        val (start, end) = config.portRange
        val ports = (start..end).shuffled()
        for (port in ports) {
            if (!active.containsKey(port) && isPortAvailable(port)) return port
        }
        throw IllegalStateException("No free port available in range $start-$end")
    }

    /** Checks if a port is available at OS level */
    private fun isPortAvailable(port: Int): Boolean {
        return try {
            ServerSocket(port).close()
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Starts a server on a free port */
    fun startServer(model: String, protocol: String = "cheetah"): Map<String, Any> {
        val freePort = getFreePort()
        val proc = spawnServer(freePort, model, protocol)

        active[freePort] = proc
        startTime[freePort] = Instant.now()
        modelByPort[freePort] = model
        protocolByPort[freePort] = protocol

        val scope = CoroutineScope(Dispatchers.IO)

        // Capture logs asynchronously
        scope.launch {
            proc.inputStream.bufferedReader().forEachLine {
                println("[server:$freePort][$model][$protocol] $it")
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
            "protocol" to protocol,
            "status" to "ok"
        )
    }

    /** Returns only currently running servers */
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

    /** Cleans up when a process exits */
    private fun cleanup(port: Int) {
        active.remove(port)
        startTime.remove(port)
        modelByPort.remove(port)
        protocolByPort.remove(port)
    }
}
