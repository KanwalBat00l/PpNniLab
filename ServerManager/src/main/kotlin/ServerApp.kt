import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.http.*
import java.io.File
import java.net.ServerSocket
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*

/**
 * SECTION 1: DATA MODELS & CONFIGURATION
 */
data class ModelConfig(
    val model_dir: String, 
    val model_cmd: String
)

data class Config(
    val hostIp: String = "127.0.0.1",
    val managerPort: Int = 8080,
    val portRange: List<Int> = listOf(9000, 9200),
    val serverLifetimeMs: Long = 120000,
    val models: Map<String, ModelConfig> = emptyMap()
) {
    val baseUrl: String get() = "http://$hostIp:$managerPort"
}

/**
 * SECTION 2: BUSINESS LOGIC (SERVER SERVICE)
 */
class ServerService(private val config: Config) {
    private val activeServers = ConcurrentHashMap<Int, Process>()
    private val metadata = ConcurrentHashMap<Int, Triple<String, String, Instant>>()

    /** Validates request and spawns a new Mock Server process */
    fun startServer(model: String, protocol: String): Map<String, Any> {
        // 1. Strict Validation
        if (protocol !in listOf("cheetah", "SCI_HE")) {
            throw IllegalArgumentException("Unsupported protocol: $protocol")
        }
        val modelCfg = config.models[model] 
            ?: throw IllegalStateException("Model configuration for '$model' not found in config.json")

        // 2. Dynamic Port Allocation
        val port = (config.portRange[0]..config.portRange[1]).shuffled().firstOrNull { 
            !activeServers.containsKey(it) && isPortAvailable(it) 
        } ?: throw IllegalStateException("RESOURCE_EXHAUSTED: No free ports in range ${config.portRange}")

        // 3. Process Execution
        val workingDir = File(modelCfg.model_dir).canonicalFile
        val scriptFile = File(workingDir, modelCfg.model_cmd).canonicalFile
        
        if (!scriptFile.exists()) {
            throw IllegalStateException("Binary execution script missing at: ${scriptFile.absolutePath}")
        }

        val pb = ProcessBuilder("bash", scriptFile.absolutePath, protocol, model, port.toString())
            .directory(workingDir)
            .redirectErrorStream(true)

        val process = pb.start()
        activeServers[port] = process
        metadata[port] = Triple(model, protocol, Instant.now())

        // 4. Async Monitor
        CoroutineScope(Dispatchers.IO).launch {
            // Log Capture
            launch { 
                process.inputStream.bufferedReader().forEachLine { println("[SubProcess:Port $port] $it") } 
            }
            
            // Timeout Watchdog
            val finished = process.waitFor(config.serverLifetimeMs, TimeUnit.MILLISECONDS)
            if (!finished) {
                println("[Manager] Port $port timed out. Forcing termination.")
                process.destroyForcibly()
            }
            cleanup(port)
        }

        return mapOf(
            "ip" to config.hostIp,
            "port" to port,
            "model" to model,
            "protocol" to protocol,
            "status" to "ok",
            "timestamp" to Instant.now().toString()
        )
    }

    private fun isPortAvailable(port: Int): Boolean = try { 
        ServerSocket(port).use { true } 
    } catch (e: Exception) { false }

    fun getStatus() = metadata.map { (port, data) ->
        mapOf(
            "port" to port,
            "model" to data.first,
            "protocol" to data.second,
            "startedAt" to data.third.toString(),
            "uptime_sec" to (Instant.now().epochSecond - data.third.epochSecond)
        )
    }

    private fun cleanup(port: Int) {
        activeServers.remove(port)
        metadata.remove(port)
    }
}

/**
 * SECTION 3: APPLICATION ENTRY POINT
 */
fun main() {
    val mapper = ObjectMapper().registerKotlinModule()
    val configFile = File("config.json")
    
    val config = try { 
        if (configFile.exists()) {
            println("✅ [System] Loading config from config.json")
            mapper.readValue<Config>(configFile) 
        } else {
            println("⚠️ [System] config.json not found, using default localhost.")
            Config()
        }
    } catch (e: Exception) { 
        println("❌ [System] Critical Config Error: ${e.message}")
        Config() 
    }

    val service = ServerService(config)

    embeddedServer(Netty, port = config.managerPort, host = "0.0.0.0") {
        install(ContentNegotiation) { jackson() }
        
        // PROFESSIONAL MANUAL INTERCEPTOR (Replacement for CallLogging)
        intercept(ApplicationCallPipeline.Monitoring) {
            val startTime = Instant.now().toEpochMilli()
            proceed()
            val duration = Instant.now().toEpochMilli() - startTime
            println("📝 [HTTP] ${call.request.httpMethod.value} ${call.request.uri} - ${call.response.status()} (${duration}ms)")
        }

        routing {
            // --- API ROUTES ---
            get("/getServer") {
                val model = call.request.queryParameters["model"] ?: "resnet50"
                val protocol = call.request.queryParameters["protocol"] ?: "cheetah"
                try {
                    call.respond(service.startServer(model, protocol))
                } catch (e: Exception) {
                    println("❌ [Error] /getServer failed: ${e.message}")
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                }
            }

            get("/status") {
                call.respond(service.getStatus())
            }

            // --- SWAGGER & STATIC ASSETS ---
            // Mapping root ensures all relative JS/CSS/JSON are found
            staticResources("/", "static", index = "index.html")
        }
        
        println("\n====================================================")
        println("🚀 PPNNI MANAGER READY")
        println("🌍 Mobile Endpoint: ${config.baseUrl}/getServer")
        println("📖 Swagger Console: ${config.baseUrl}/index.html")
        println("====================================================\n")

    }.start(wait = true)
}