package com.example.server.config

/** Model configuration loaded from config.json */
data class ModelConfig(
    val model_dir: String,
    val model_cmd: String
)

/** Global configuration for the server manager */
data class Config(
    val hostIp: String = "127.0.0.1",
    val managerPort: Int = 8080,
    val portPool: List<Int> = listOf(6000, 6001, 6002),
    val serverLifetimeMs: Long = 300_000,
    val models: Map<String, ModelConfig> = emptyMap()
)
