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
import java.net.URI

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

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { imageUri ->
                val model = spModel.selectedItem.toString()
                val protocol = spProtocol.selectedItem.toString()

                MockImageInputManager.processImage(this, imageUri, model, protocol) { file ->
                    appendLog("success", "✅ Preprocessing complete: ${file.name}")
                    runMockClientAfterInputReady()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Ensure workspace directory exists
            File(filesDir, "pretrained").mkdirs()

            setContentView(R.layout.activity_main)

            clientManager = ClientManager(this)
            outputText = findViewById(R.id.tvResult)
            scrollView = findViewById(R.id.scrollOutput)
            spModel = findViewById(R.id.spModel)
            spProtocol = findViewById(R.id.spProtocol)
            etServerUrl = findViewById(R.id.etServerUrl)
            btnConnectServer = findViewById(R.id.btnConnectServer)
            btnRunClient = findViewById(R.id.btnSelectImage)

            outputText.movementMethod = ScrollingMovementMethod()

            // Connect Button Logic
            btnConnectServer.setOnClickListener {
                lifecycleScope.launch {
                    resetLog()
                    val baseUrl = etServerUrl.text.toString().ifBlank { "http://10.0.2.2:8080" }
                    appendLog("info", "🛰️ Querying Manager: $baseUrl")

                    clientManager.requestServer(baseUrl, spModel.selectedItem.toString(), spProtocol.selectedItem.toString())
                        .onSuccess {
                            currentServer = it
                            appendLog("success", "✅ Reserved Port: ${it.port}")
                            btnRunClient.isEnabled = true
                            btnRunClient.alpha = 1f
                        }.onFailure {
                            appendLog("error", "❌ Manager Error: ${it.message}")
                        }
                }
            }

            btnRunClient.setOnClickListener {
                imagePicker.launch("image/*")
            }

        } catch (e: Exception) {
            // Fallback if layout fails (prevents black screen)
            val tv = TextView(this)
            tv.text = "Fatal Init Error: ${e.message}"
            setContentView(tv)
        }
    }

    private fun runMockClientAfterInputReady() {
        val server = currentServer ?: return

        // Extract IP from the input field to ensure we route correctly to the Mac
        val userUrl = etServerUrl.text.toString().ifBlank { "http://10.0.2.2:8080" }
        val targetHost = try { URI(userUrl).host ?: "10.0.2.2" } catch (e: Exception) { "10.0.2.2" }

        val model = spModel.selectedItem.toString()
        val proto = spProtocol.selectedItem.toString()

        appendLog("info", "🚀 Connecting to $targetHost:${server.port}...")

        lifecycleScope.launch {
            try {
                val output = clientManager.runMockClient(proto, model, targetHost, server.port)
                appendLog("success", "--- NATIVE OUTPUT ---\n$output")

                val prediction = clientManager.parseClientOutput(output)
                appendLog("info", "📊 $prediction")
            } catch (e: Exception) {
                appendLog("error", "❌ JNI Execution Error: ${e.message}")
            }
        }
    }

    private fun resetLog() { outputText.text = "" }

    private fun appendLog(type: String, message: String) {
        val color = when (type) {
            "success" -> 0xFF008000.toInt()
            "error" -> 0xFFFF0000.toInt()
            "info" -> 0xFF0000FF.toInt()
            else -> 0xFF333333.toInt()
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