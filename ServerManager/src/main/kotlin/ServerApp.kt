package com.example.server

import com.example.server.config.Config
import com.example.server.service.ServerService
import com.example.server.controller.serverRoutes
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import java.io.File

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
        serverRoutes(service)
    }.start(wait = true)
}
