package com.autoglm.voice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.autoglm.voice.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * 主界面
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        checkPermissions()
    }

    private fun setupUI() {
        binding.appName.text = getString(R.string.app_name)
        
        binding.btnStartService.setOnClickListener {
            startVoiceService()
        }
        
        binding.btnSettings.setOnClickListener {
            openSettings()
        }
        
        binding.btnApiConfig.setOnClickListener {
            showApiConfigDialog()
        }
        
        // 检查服务状态
        checkServiceStatus()
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        
        // 检查录音权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }
        
        // 检查悬浮窗权限
        if (!Settings.canDrawOverlays(this)) {
            permissions.add("overlay")
        }
        
        // 检查通知权限（Android 13+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        if (permissions.isNotEmpty()) {
            showPermissionDialog(permissions)
        }
    }

    private fun showPermissionDialog(permissions: List<String>) {
        MaterialAlertDialogBuilder(this)
            .setTitle("需要权限")
            .setMessage("本应用需要录音和悬浮窗权限才能正常工作")
            .setPositiveButton("去授权") { _, _ ->
                requestPermissions(permissions)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun requestPermissions(permissions: List<String>) {
        val androidPermissions = permissions.filter { it != "overlay" }
        
        if (androidPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                androidPermissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
        
        if (permissions.contains("overlay")) {
            requestOverlayPermission()
        }
    }

    private fun requestOverlayPermission() {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            checkServiceStatus()
        }
    }

    private fun startVoiceService() {
        if (!AutoGLMAccessibilityService.isServiceRunning()) {
            Toast.makeText(this, "请先开启无障碍服务", Toast.LENGTH_LONG).show()
            openAccessibilitySettings()
            return
        }
        
        val intent = Intent(this, VoiceService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        
        Toast.makeText(this, "语音助手已启动", Toast.LENGTH_SHORT).show()
        binding.btnStartService.isEnabled = false
        binding.btnStartService.text = "运行中"
    }

    private fun openSettings() {
        MaterialAlertDialogBuilder(this)
            .setTitle("设置")
            .setItems(arrayOf("无障碍服务", "悬浮窗权限", "API 配置")) { _, which ->
                when (which) {
                    0 -> openAccessibilitySettings()
                    1 -> requestOverlayPermission()
                    2 -> showApiConfigDialog()
                }
            }
            .show()
    }

    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showApiConfigDialog() {
        val prefs = getSharedPreferences("autoglm", MODE_PRIVATE)
        val currentKey = prefs.getString("api_key", "") ?: ""
        
        val editText = android.widget.EditText(this).apply {
            setText(currentKey)
            hint = "输入智谱 API Key"
        }
        
        MaterialAlertDialogBuilder(this)
            .setTitle("配置 API Key")
            .setMessage("请输入智谱 AI 的 API Key")
            .setView(editText)
            .setPositiveButton("保存") { _, _ ->
                val newKey = editText.text.toString().trim()
                prefs.edit().putString("api_key", newKey).apply()
                Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .setNeutralButton("获取 Key") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://open.bigmodel.cn")
                startActivity(intent)
            }
            .show()
    }

    private fun checkServiceStatus() {
        val isRunning = AutoGLMAccessibilityService.isServiceRunning()
        binding.btnStartService.isEnabled = isRunning
        binding.btnStartService.text = if (isRunning) "运行中" else "启动服务"
        binding.serviceStatus.text = if (isRunning) "无障碍服务：已开启" else "无障碍服务：未开启"
    }

    override fun onResume() {
        super.onResume()
        checkServiceStatus()
    }
}
