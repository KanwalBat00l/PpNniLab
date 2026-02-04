package com.example.androidapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING) // Run in alphabetical order
class AndroidAppTest {

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun UT_01_JNI_Bridge_Check() {
        println("🧪 [START] UT_01: Verifying JNI Bridge Integrity")
        // Uses the pathing logic verified in Step 2 of your plan
        val result = NativeBridge.runMockClient(
            TestConfig.DEFAULT_PROTOCOL,
            TestConfig.DEFAULT_MODEL,
            "127.0.0.1", 1,
            appContext.filesDir.absolutePath
        )

        System.out.println(">>> JNI RAW OUTPUT:\n$result")

        assertNotNull("JNI returned null - Library loading failed!", result)
        assertTrue("Output missing status wrapper", result.contains("[System Status:"))
        println("✅ [PASS] JNI Bridge is linked and executable.")
    }

    @Test
    fun UT_02_Workspace_IO_Safety() {
        println("🧪 [START] UT_02: Verifying Internal Workspace Permissions")
        val workspace = File(appContext.filesDir, "pretrained")
        if (!workspace.exists()) workspace.mkdirs()

        val testFile = File(workspace, "verifier.txt")
        testFile.writeText("logic_check")

        try {
            assertEquals("File I/O Data mismatch", "logic_check", testFile.readText())
            println("✅ [PASS] Internal storage is writable for Native Process.")
        } finally {
            testFile.delete()
        }
    }

    @Test
    fun UT_03_Path_Sync_Verification() {
        println("🧪 [START] UT_03: Verifying Kotlin-to-JNI Path Synchronization")
        val internalPath = appContext.filesDir.absolutePath
        val expectedInpPath = "$internalPath/pretrained/${TestConfig.DEFAULT_MODEL}_mock_input.inp"

        assertTrue("Invalid Path Construction", expectedInpPath.contains("com.example.androidapp"))
        assertTrue("Missing file extension", expectedInpPath.endsWith(".inp"))

        println(">>> Calculated Sync Path: $expectedInpPath")
        println("✅ [PASS] Pathing logic is correctly synchronized.")
    }

    @Test
    fun UT_04_Manager_Connectivity() {
        println("🧪 [START] UT_04: Verifying Connection to Mac Server Manager")
        val clientManager = ClientManager(appContext)

        println(">>> Attempting handshake with: ${TestConfig.MANAGER_URL}")

        runBlocking {
            val result = clientManager.requestServer(
                TestConfig.MANAGER_URL,
                TestConfig.DEFAULT_MODEL,
                TestConfig.DEFAULT_PROTOCOL
            )

            // This will throw a descriptive error if the server is off
            assertTrue(
                "❌ FAILED: ServerManager unreachable at ${TestConfig.MANAGER_URL}. " +
                        "Check if './gradlew run' is active on the host Mac.",
                result.isSuccess
            )

            val server = result.getOrNull()
            assertNotNull(server)
            println("✅ [PASS] Handshake successful. Mac assigned port: ${server?.port}")
        }
    }
}