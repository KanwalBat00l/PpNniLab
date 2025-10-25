package com.example.androidapp

import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.graphics.BitmapFactory
import android.text.method.ScrollingMovementMethod
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.graphics.Color

/**
 * MainActivity:
 * Handles UI interaction, user input, image preview, and log display.
 * Delegates all network and computation logic to ClientManager.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var clientManager: ClientManager
    private lateinit var outputText: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var ivPreview: ImageView
    private var currentServer: ServerResponse? = null

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { displayAndRunClient(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // UI references
        clientManager = ClientManager(this)
        outputText = findViewById(R.id.tvResult)
        scrollView = findViewById(R.id.scrollOutput)
        ivPreview = findViewById(R.id.ivPreview)

        val requestBtn = findViewById<Button>(R.id.btnConnectServer)
        val selectBtn = findViewById<Button>(R.id.btnSelectImage)
        val etServerUrl = findViewById<EditText>(R.id.etServerUrl)
        val etModel = findViewById<EditText>(R.id.etModel)

        outputText.movementMethod = ScrollingMovementMethod()

        /** Step 1: Connect to server */
        requestBtn.setOnClickListener {
            lifecycleScope.launch {
                restLog()
                val baseUrl = etServerUrl.text.toString().ifBlank { "http://10.0.2.2:8080" }
                val modelName = etModel.text.toString().ifBlank { "default" }
                appendLog("info", "Requesting server...")

                val result = clientManager.requestServer(baseUrl, modelName)
                result.onSuccess {
                    currentServer = it
                    appendLog("success", "Connected to ${it.ip}:${it.port} (model: $modelName)")
                    selectBtn.isEnabled = true
                }.onFailure {
                    appendLog("error", "Connection failed: ${it.message}")
                }
            }
        }

        /** Step 2: Select and process image */
        selectBtn.setOnClickListener {
            imagePicker.launch("image/*")
        }
    }

    /** Displays selected image and triggers secure computation via ClientManager */
    private fun displayAndRunClient(uri: Uri) {
        contentResolver.openInputStream(uri)?.use { stream ->
            val bitmap = BitmapFactory.decodeStream(stream)
            ivPreview.setImageBitmap(bitmap)
            ivPreview.adjustViewBounds = true
            ivPreview.scaleType = ImageView.ScaleType.FIT_CENTER
        }

        val server = currentServer ?: return
        appendLog("info", "Running client...")
        lifecycleScope.launch {
            val result = clientManager.runNativeClient(server.ip, server.port, uri)
            result.onSuccess {
                appendLog("success", "Output:\n$it")
            }.onFailure {
                appendLog("error", "Failed:\n${it.message}")
            }
        }
    }

    private fun restLog() {
        outputText.text = ""
    }

    /** Adds colored log messages to UI and auto-scrolls */
    private fun appendLog(type: String, message: String) {
        val color = when (type) {
            "success" -> Color.parseColor("#008000")
            "error" -> Color.parseColor("#FF0000")
            else -> Color.parseColor("#333333")
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
