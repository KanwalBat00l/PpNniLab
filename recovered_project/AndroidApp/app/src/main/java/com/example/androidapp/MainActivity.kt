package com.example.androidapp

import android.graphics.BitmapFactory
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

    // Temporary storage for selected image URI
    private var selectedImageUri: Uri? = null

    // --- Image picker ---
    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { imageUri ->
                selectedImageUri = imageUri
                // Process image and create mock .inp file
                MockImageInputManager.processImage(
                    context = this,
                    imageUri = imageUri,
                    model = spModel.selectedItem.toString(),
                    protocol = spProtocol.selectedItem.toString()
                ) { file ->
                    appendLog("success", "Mock input file ready: ${file.name}")
                    // Now run the client
                    runMockClientAfterInputReady(file)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PretrainedFilesManager.copyPretrainedFiles(this)

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
        btnRunClient.isEnabled = false
        btnRunClient.alpha = 0.4f

        // --- Connect to server ---
        btnConnectServer.setOnClickListener {
            lifecycleScope.launch {
                resetLog()
                val baseUrl = etServerUrl.text.toString().ifBlank { "http://10.0.2.2:8080" }
                val model = spModel.selectedItem.toString()
                val protocol = spProtocol.selectedItem.toString()
                appendLog("info", "Requesting server...")

                val result = clientManager.requestServer(baseUrl, model, protocol)
                result.onSuccess {
                    currentServer = it
                    appendLog(
                        "success",
                        "Server returned IP: ${it.ip}, Port: ${it.port}, Model: ${it.model}, Protocol: ${it.protocol}"
                    )
                    btnRunClient.isEnabled = true
                    btnRunClient.alpha = 1f
                }.onFailure {
                    appendLog("error", "Connection failed: ${it.message}")
                    btnRunClient.isEnabled = false
                    btnRunClient.alpha = 0.4f
                }
            }
        }

        // --- Run mock client button ---
        btnRunClient.setOnClickListener {
            // Launch image picker first
            imagePicker.launch("image/*")
        }
    }

    private fun runMockClientAfterInputReady(file: java.io.File) {
        val server = currentServer ?: run {
            appendLog("error", "No server connected")
            return
        }

        val model = spModel.selectedItem.toString()
        val protocol = spProtocol.selectedItem.toString()

        appendLog("info", "Running mock client...")

        lifecycleScope.launch {
            val output = clientManager.runMockClient(protocol, model, server.ip, server.port)
            appendLog("success", "Output:\n$output")

            // Optional: parse last numbers as predicted class
            val predicted = clientManager.parseClientOutput(output)
            appendLog("info", "Predicted class: $predicted")
        }
    }

    // --- Logging helpers ---
    private fun resetLog() {
        outputText.text = ""
    }

    private fun appendLog(type: String, message: String) {
        val color = when (type) {
            "success" -> 0xFF008000.toInt()
            "error" -> 0xFFFF0000.toInt()
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
