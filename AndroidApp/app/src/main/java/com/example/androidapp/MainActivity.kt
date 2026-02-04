package com.example.androidapp

import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.ScrollingMovementMethod
import android.text.style.ForegroundColorSpan
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var clientManager: ClientManager
    private lateinit var outputText: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var spModel: Spinner
    private lateinit var spProtocol: Spinner
    private lateinit var etServerUrl: EditText
    private lateinit var btnConnectServer: Button
    private lateinit var btnRunClient: Button

    private var currentServer: ServerResponse? = null

    // --- Image picker ---
    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { imageUri ->
                // 1. Process image and create mock .inp file in the "pretrained" workspace
                MockImageInputManager.processImage(
                    context = this,
                    imageUri = imageUri,
                    model = spModel.selectedItem.toString(),
                    protocol = spProtocol.selectedItem.toString()
                ) { file ->
                    appendLog("success", "✅ Image processed: ${file.name}")
                    // 2. Run the native client now that input is ready
                    runMockClientAfterInputReady()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure workspace directory exists for C++ I/O
        File(filesDir, "pretrained").mkdirs()

        setContentView(R.layout.activity_main)

        // Initialize Views
        clientManager = ClientManager(this)
        outputText = findViewById(R.id.tvResult)
        scrollView = findViewById(R.id.scrollOutput)
        spModel = findViewById(R.id.spModel)
        spProtocol = findViewById(R.id.spProtocol)
        etServerUrl = findViewById(R.id.etServerUrl)
        btnConnectServer = findViewById(R.id.btnConnectServer)
        btnRunClient = findViewById(R.id.btnSelectImage)

        outputText.movementMethod = ScrollingMovementMethod()
        btnRunClient.isEnabled = false
        btnRunClient.alpha = 0.4f

        // --- Connect to Server Manager ---
        btnConnectServer.setOnClickListener {
            lifecycleScope.launch {
                resetLog()
                val baseUrl = etServerUrl.text.toString().ifBlank { "http://10.0.2.2:8080" }
                val model = spModel.selectedItem.toString()
                val protocol = spProtocol.selectedItem.toString()

                appendLog("info", "🛰️ Requesting server from Manager...")

                val result = clientManager.requestServer(baseUrl, model, protocol)
                result.onSuccess {
                    currentServer = it
                    appendLog("success", "✅ Server Ready: ${it.ip}:${it.port}")
                    btnRunClient.isEnabled = true
                    btnRunClient.alpha = 1f
                }.onFailure {
                    appendLog("error", "❌ Connection failed: ${it.message}")
                    btnRunClient.isEnabled = false
                    btnRunClient.alpha = 0.4f
                }
            }
        }

        // --- Run Mock Client ---
        btnRunClient.setOnClickListener {
            imagePicker.launch("image/*")
        }
    }

    private fun runMockClientAfterInputReady() {
        val server = currentServer ?: run {
            appendLog("error", "❌ No server connected")
            return
        }

        val model = spModel.selectedItem.toString()
        val protocol = spProtocol.selectedItem.toString()

        appendLog("info", "🚀 Running Secure Inference...")

        lifecycleScope.launch {
            // Using ClientManager wrapper which now handles the filesDir correctly
            val output = clientManager.runMockClient(protocol, model, server.ip, server.port)
            appendLog("success", output)

            val predicted = clientManager.parseClientOutput(output)
            appendLog("info", "📊 $predicted")
        }
    }
    // --- Logging Helpers ---
    private fun resetLog() {
        outputText.text = ""
    }

    private fun appendLog(type: String, message: String) {
        val color = when (type) {
            "success" -> 0xFF008000.toInt() // Green
            "error" -> 0xFFFF0000.toInt()   // Red
            "info" -> 0xFF0000FF.toInt()    // Blue
            else -> 0xFF333333.toInt()      // Dark Gray
        }
        val spannable = SpannableStringBuilder(outputText.text)
        if (spannable.isNotEmpty()) spannable.append("\n")
        val start = spannable.length
        spannable.append(message)
        spannable.setSpan(ForegroundColorSpan(color), start, spannable.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        outputText.text = spannable
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }
}