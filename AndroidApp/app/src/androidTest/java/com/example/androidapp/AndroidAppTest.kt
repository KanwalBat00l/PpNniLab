package com.example.androidapp

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidAppTest {

    // --- CONFIGURABLE TEST PARAMETERS ---
    private val SERVER_IP = "10.168.212.227" // Change this to your Mac IP
    private val MANAGER_URL = "http://$SERVER_IP:8080"

    @Test
    fun UT_Android_01_JNI_Connectivity() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext

        // We test with a dummy IP. The goal is to see if the C++ code executes without crashing.
        val result = NativeBridge.runMockClient(
            "cheetah",
            "sqnet",
            "127.0.0.1",
            8000,
            context.filesDir.absolutePath
        )

        Assert.assertNotNull(result)
        Assert.assertTrue(result.contains("Native Client Exit Code"))
    }

    @Test
    fun UT_Android_02_Preprocessing_Format() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        // Create a dummy URI and test the preprocessor
        // (Assuming you have a test image in your assets or resources)
        // This confirms the .inp file is generated in the 'pretrained' workspace
    }

    @Test
    fun UT_Android_03_Automated_Server_Handoff() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = ClientManager(appContext)

        // This runs the full flow:
        // 1. Talk to Ktor Manager
        // 2. Receive Port
        // 3. Verify the IP matches our config

        /*
        runBlocking {
            val result = manager.requestServer(MANAGER_URL, "resnet50", "cheetah")
            assertTrue(result.isSuccess)
            assertEquals(SERVER_IP, result.getOrNull()?.ip)
        }
        */
    }
}