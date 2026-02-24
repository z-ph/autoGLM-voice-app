package com.autoglm.voice

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.*

/**
 * 语音服务
 * 悬浮按钮 + 语音识别 + API 调用
 */
class VoiceService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isRecording = false
    
    private val handler = Handler(Looper.getMainLooper())
    private val client = OkHttpClient()
    
    companion object {
        private const val CHANNEL_ID = "autoglm_voice_channel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        createFloatingButton()
        initSpeechRecognizer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        removeFloatingButton()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AutoGLM 语音服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "语音助手后台服务"
            }
            
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }
        
        return builder
            .setContentTitle("AutoGLM 语音助手")
            .setContentText("点击悬浮按钮开始语音")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
    }

    private fun createFloatingButton() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        val params = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }
        
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_button, null)
        val btn = floatingView?.findViewById<TextView>(R.id.btnVoice)
        
        btn?.setOnClickListener {
            if (!isRecording) {
                startVoiceRecognition()
            }
        }
        
        btn?.setOnLongClickListener {
            Toast.makeText(this, "长按关闭服务", Toast.LENGTH_SHORT).show()
            stopSelf()
            true
        }
        
        try {
            windowManager.addView(floatingView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeFloatingButton() {
        try {
            floatingView?.let { windowManager.removeView(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "语音识别不可用", Toast.LENGTH_SHORT).show()
            return
        }
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            
            override fun onError(error: Int) {
                isRecording = false
                updateButtonState()
                Toast.makeText(this@VoiceService, "识别失败：${getErrorText(error)}", Toast.LENGTH_SHORT).show()
            }
            
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    Toast.makeText(this@VoiceService, "识别：$text", Toast.LENGTH_SHORT).show()
                    sendToAutoGLM(text)
                }
                isRecording = false
                updateButtonState()
            }
            
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startVoiceRecognition() {
        if (isRecording) return
        
        isRecording = true
        updateButtonState()
        Toast.makeText(this, "请说话...", Toast.LENGTH_SHORT).show()
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            isRecording = false
            updateButtonState()
        }
    }

    private fun updateButtonState() {
        handler.post {
            val btn = floatingView?.findViewById<TextView>(R.id.btnVoice)
            btn?.text = if (isRecording) "⏺️" else "🎤"
        }
    }

    private fun sendToAutoGLM(text: String) {
        val prefs = getSharedPreferences("autoglm", MODE_PRIVATE)
        val apiKey = prefs.getString("api_key", "") ?: ""
        
        if (apiKey.isEmpty()) {
            Toast.makeText(this, "请先配置 API Key", Toast.LENGTH_LONG).show()
            return
        }
        
        Toast.makeText(this, "发送到 AutoGLM...", Toast.LENGTH_SHORT).show()
        
        val json = JSONObject().apply {
            put("query", text)
            put("device_type", "android")
        }
        
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = RequestBody.create(mediaType, json.toString())
        
        val request = Request.Builder()
            .url("https://open.bigmodel.cn/api/paas/v4/autoglm")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                handler.post {
                    Toast.makeText(this@VoiceService, "API 请求失败", Toast.LENGTH_SHORT).show()
                }
            }
            
            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                if (responseData != null) {
                    try {
                        val result = JSONObject(responseData)
                        parseAndExecute(result)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        })
    }

    private fun parseAndExecute(result: JSONObject) {
        val commands = if (result.has("commands")) {
            result.getJSONArray("commands")
        } else if (result.has("action")) {
            org.json.JSONArray().put(result.getJSONObject("action"))
        } else {
            null
        }
        
        commands?.let {
            handler.post {
                for (i in 0 until it.length()) {
                    val cmd = it.getJSONObject(i)
                    executeCommand(cmd)
                    Thread.sleep(500)
                }
                Toast.makeText(this, "执行完成", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun executeCommand(cmd: JSONObject) {
        val service = AutoGLMAccessibilityService.instance ?: return
        
        val type = cmd.optString("type").ifEmpty { cmd.optString("action", "") }
        
        when (type) {
            "click" -> {
                if (cmd.has("x") && cmd.has("y")) {
                    service.performClick(cmd.getInt("x"), cmd.getInt("y"))
                } else if (cmd.has("text")) {
                    service.clickByText(cmd.getString("text"))
                }
            }
            "swipe" -> {
                service.performSwipe(
                    cmd.optInt("x1", 500),
                    cmd.optInt("y1", 1500),
                    cmd.optInt("x2", 500),
                    cmd.optInt("y2", 500),
                    cmd.optLong("duration", 1000)
                )
            }
            "input", "type" -> {
                service.inputText(cmd.optString("text", ""))
            }
            "back" -> service.performBack()
            "home" -> service.performHome()
            "open_app", "launch" -> {
                val appName = cmd.optString("app_name").ifEmpty { cmd.optString("app", "") }
                // 需要转换为包名
            }
        }
    }

    private fun getErrorText(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "录音错误"
            SpeechRecognizer.ERROR_CLIENT -> "客户端错误"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "权限不足"
            SpeechRecognizer.ERROR_NETWORK -> "网络错误"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
            SpeechRecognizer.ERROR_NO_MATCH -> "未识别到内容"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别服务忙"
            SpeechRecognizer.ERROR_SERVER -> "服务器错误"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "说话超时"
            else -> "未知错误 ($errorCode)"
        }
    }
}
