import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.http.*
import java.io.File
import java.net.ServerSocket
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import org.slf4j.event.*

// CONFIGURATION MODELS
data class ModelConfig(val model_dir: String, val model_cmd: String)
data class Config(
    val hostIp: String = "127.0.0.1",
    val managerPort: Int = 8080,
    val portRange: List<Int> = listOf(9000, 9200),
    val serverLifetimeMs: Long = 120000,
    val models: Map<String, ModelConfig> = emptyMap()
) {
    val baseUrl: String get() = "http://$hostIp:$managerPort"
}

// SERVICE LOGIC
class ServerService(private val config: Config) {
    private val activeServers = ConcurrentHashMap<Int, Process>()
    private val metadata = ConcurrentHashMap<Int, Triple<String, String, Instant>>()

    fun startServer(model: String, protocol: String): Map<String, Any> {
        if (protocol !in listOf("cheetah", "SCI_HE")) throw IllegalArgumentException("Invalid protocol")
        val modelCfg = config.models[model] ?: throw IllegalStateException("Model '$model' not found in config.json")

        val port = (config.portRange[0]..config.portRange[1]).shuffled().firstOrNull { 
            !activeServers.containsKey(it) && isPortAvailable(it) 
        } ?: throw IllegalStateException("No ports available in range ${config.portRange}")

        val workingDir = File(modelCfg.model_dir).canonicalFile
        val pb = ProcessBuilder("bash", modelCfg.model_cmd, protocol, model, port.toString())
            .directory(workingDir)
            .redirectErrorStream(true)

        val process = pb.start()
        activeServers[port] = process
        metadata[port] = Triple(model, protocol, Instant.now())

        CoroutineScope(Dispatchers.IO).launch {
            launch { process.inputStream.bufferedReader().forEachLine { println("[Bin-Port:$port] $it") } }
            process.waitFor(config.serverLifetimeMs, TimeUnit.MILLISECONDS)
            process.destroyForcibly()
            activeServers.remove(port)
            metadata.remove(port)
        }

        return mapOf("ip" to config.hostIp, "port" to port, "model" to model, "protocol" to protocol, "status" to "ok")
    }

    private fun isPortAvailable(port: Int): Boolean = try { ServerSocket(port).use { true } } catch (e: Exception) { false }
    fun getStatus() = metadata.map { (port, data) ->
        mapOf("port" to port, "model" to data.first, "protocol" to data.second, "startedAt" to data.third.toString())
    }
}

// APP ENTRY
fun main() {
    val mapper = ObjectMapper().registerKotlinModule()
    val configFile = File("config.json")
    val config = try { 
        if (configFile.exists()) mapper.readValue<Config>(configFile) else Config() 
    } catch (e: Exception) { 
        println("⚠️ Config Load Error: ${e.message}")
        Config() 
    }
    
    val service = ServerService(config)

    embeddedServer(Netty, port = config.managerPort, host = "0.0.0.0") {
        install(ContentNegotiation) { jackson() }
        
        // This will print every HTTP request to your terminal
        install(CallLogging) {
            level = Level.INFO
        }

        routing {
            get("/getServer") {
                val model = call.request.queryParameters["model"] ?: "resnet50"
                val protocol = call.request.queryParameters["protocol"] ?: "cheetah"
                try {
                    val result = service.startServer(model, protocol)
                    call.respond(result)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                }
            }

            get("/status") {
                call.respond(service.getStatus())
            }

            // Restore the /swagger prefix. 
            // This maps http://IP:PORT/swagger/index.html to src/main/resources/static
            staticResources("/swagger", "static", index = "index.html")
            
            // Also keep a root redirect for convenience
            get("/") {
                call.respondRedirect("/swagger/index.html")
            }
        }
        
        println("\n🚀 PPNNI MANAGER STARTED")
        println("📱 MOBILE API:  ${config.baseUrl}/getServer")
        println("📖 SWAGGER UI:  ${config.baseUrl}/swagger/index.html")
        println("----------------------------------------------------\n")
    }.start(wait = true)
}