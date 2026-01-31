import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.DisplayName

class ServerServiceTest {

    private val testConfig = Config(
        hostIp = "127.0.0.1",
        portRange = listOf(9900, 9910),
        models = mapOf("resnet50" to ModelConfig(".", "echo"))
    )

    @Test
    @DisplayName("🧪 UT_Server_01: Security Validation - Protocol Whitelist")
    fun testProtocolValidation() {
        val service = ServerService(testConfig)
        val invalidProto = "malicious_protocol"
        
        println("   [CHECK] Sending unapproved protocol: '$invalidProto'")
        
        val exception = assertThrows<IllegalArgumentException> {
            service.startServer("resnet50", invalidProto)
        }
        
        println("   [ASSERT] Caught expected exception: ${exception.message}")
        assertTrue(exception.message!!.contains("Invalid protocol"))
    }

    @Test
    @DisplayName("🧪 UT_Server_02: Orchestrator State - Initial Registry")
    fun testInitialStatus() {
        val service = ServerService(testConfig)
        
        println("   [CHECK] Inspecting server registry before any client requests...")
        val status = service.getStatus()
        
        println("   [ASSERT] Registry size is: ${status.size}")
        assertTrue(status.isEmpty(), "Registry should be empty on startup")
    }

    @Test
    @DisplayName("🧪 UT_Server_03: Dynamic Configuration - URL Generation")
    fun testUrlGeneration() {
        val testIp = "10.0.0.1"
        val testPort = 7000
        val cfg = Config(hostIp = testIp, managerPort = testPort)
        
        println("   [INPUT] Host IP: $testIp, Port: $testPort")
        
        val resultUrl = cfg.baseUrl
        
        println("   [ASSERT] Generated BaseURL: $resultUrl")
        assertEquals("http://$testIp:$testPort", resultUrl)
    }
}