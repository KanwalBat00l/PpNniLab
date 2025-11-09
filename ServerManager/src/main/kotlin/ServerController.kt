package com.example.server.controller

import com.example.server.service.ServerService
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.http.*

fun Application.serverRoutes(service: ServerService) {
    routing {
        get("/getServer") {
            val model = call.request.queryParameters["model"] ?: "resnet50"
            val protocol = call.request.queryParameters["protocol"] ?: "cheetah"

            try {
                val info = service.startServer(model, protocol)
                call.respond(info)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        get("/status") {
            call.respond(service.getStatus())
        }
    }
}
