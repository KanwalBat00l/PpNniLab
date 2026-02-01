import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.BeforeAll
import java.io.File

class ServerServiceTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() {
            // FIX: We make the mock script sleep for 5 seconds.
            // This ensures the port stays "busy" in the Manager's memory
            // long enough for the Exhaustion test to verify the limit.
            val dummyScript = File("mock_echo.sh")
            dummyScript.writeText("#!/bin/bash\nsleep 5\necho mock")
            dummyScript.setExecutable(true)
        }
    }

    private val testConfig = Config(
        hostIp = "127.0.0.1",
        portRange = listOf(9900, 9901), // Only 2 ports available
        models = mapOf("resnet50" to ModelConfig(".", "mock_echo.sh"))
    )

    @Test
    @DisplayName("🧪 UT_Server_01: Input Sanitization")
    fun testValidation() {
        val service = ServerService(testConfig)
        println(">>> Testing validation of protocol strings...")
        assertThrows<IllegalArgumentException> { 
            service.startServer("resnet50", "unapproved") 
        }
    }

    @Test
    @DisplayName("🧪 UT_Server_02: Atomic Port Exhaustion")
    fun testExhaustion() {
        val service = ServerService(testConfig)
        println(">>> Filling port range to test overflow protection...")
        
        // These will now stay in the 'activeServers' map because of the 'sleep 5'
        service.startServer("resnet50", "cheetah")
        service.startServer("resnet50", "cheetah")
        
        // NOW this will correctly throw the exception
        val error = assertThrows<IllegalStateException> {
            service.startServer("resnet50", "cheetah")
        }
        
        println(">>> Caught Expected Error: ${error.message}")
        assertTrue(error.message!!.contains("No free ports"))
    }

    @Test
    @DisplayName("🧪 UT_Server_03: Metadata & Uptime Tracking")
    fun testMetadata() {
        val service = ServerService(testConfig)
        println(">>> Verifying instance tracking logic...")
        service.startServer("resnet50", "cheetah")
        
        val status = service.getStatus()
        assertFalse(status.isEmpty())
        assertEquals("resnet50", status[0]["model"])
    }

    @Test
    @DisplayName("🧪 UT_Server_04: Environment Portability")
    fun testConfig() {
        println(">>> Verifying dynamic URL construction...")
        val cfg = Config(hostIp = "api.ppnni.eu", managerPort = 443)
        assertEquals("http://api.ppnni.eu:443", cfg.baseUrl)
    }
}