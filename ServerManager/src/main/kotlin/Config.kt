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
    val portRange: List<Int> = listOf(9000, 9200),
    val serverLifetimeMs: Long = 120000,
    val models: Map<String, ModelConfig> = emptyMap()
) {
    val baseUrl: String 
        get() = "http://$hostIp:$managerPort"
}