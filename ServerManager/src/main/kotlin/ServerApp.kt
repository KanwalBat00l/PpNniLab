package com.example.server

import com.example.server.config.Config
import com.example.server.controller.serverRoutes
import com.example.server.service.ServerService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import java.io.File

/** Application entry point */
fun main() {
    val mapper = ObjectMapper().registerKotlinModule()
    val configFile = File("config.json")

    val config: Config = if (configFile.exists()) {
        try { mapper.readValue(configFile) } 
        catch (e: Exception) {
            println("❌ Failed to parse config.json: ${e.message}")
            Config()
        }
    } else {
        println("⚠️ config.json not found — using defaults.")
        Config()
    }

    val service = ServerService(config)

    println("🚀 Server Manager starting on ${config.hostIp}:${config.managerPort}")

    embeddedServer(Netty, port = config.managerPort) {
        install(ContentNegotiation) { jackson() }

        // ✅ Register your API routes in Application scope
        serverRoutes(service)

        routing {
            // Serve Swagger UI static files
            static("/swagger") {
                resources("static")
            }

            // Serve openapi.json
            get("/openapi.json") {
                val file = File("src/main/resources/static/openapi.json")
                if (file.exists()) {
                    call.respondFile(file)
                } else {
                    call.respondText("openapi.json not found", status = HttpStatusCode.NotFound)
                }
            }
        }
    }.start(wait = true)
}
