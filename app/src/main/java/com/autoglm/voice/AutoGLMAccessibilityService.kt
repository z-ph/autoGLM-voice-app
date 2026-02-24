package com.autoglm.voice

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.os.Handler
import android.os.Looper

/**
 * AutoGLM 无障碍服务
 * 负责执行手机操作（点击、滑动、输入等）
 */
class AutoGLMAccessibilityService : AccessibilityService() {

    companion object {
        var instance: AutoGLMAccessibilityService? = null
            private set
        
        fun isServiceRunning(): Boolean = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        
        // 配置服务
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.DEFAULT or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 可选：处理辅助功能事件
    }

    override fun onInterrupt() {
        // 服务被中断
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    /**
     * 执行点击操作
     */
    fun performClick(x: Int, y: Int): Boolean {
        return try {
            val result = dispatchGesture(
                android.accessibilityservice.GestureDescription.Builder()
                    .addStroke(
                        android.accessibilityservice.GestureDescription.StrokeDescription(
                            android.graphics.Path().apply { moveTo(x.toFloat(), y.toFloat()) },
                            0,
                            100
                        )
                    )
                    .build(),
                null,
                null
            )
            result
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 执行滑动操作
     */
    fun performSwipe(x1: Int, y1: Int, x2: Int, y2: Int, duration: Long = 1000): Boolean {
        return try {
            val path = android.graphics.Path().apply {
                moveTo(x1.toFloat(), y1.toFloat())
                lineTo(x2.toFloat(), y2.toFloat())
            }
            
            dispatchGesture(
                android.accessibilityservice.GestureDescription.Builder()
                    .addStroke(
                        android.accessibilityservice.GestureDescription.StrokeDescription(
                            path,
                            0,
                            duration
                        )
                    )
                    .build(),
                null,
                null
            )
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 通过文本查找并点击
     */
    fun clickByText(text: String): Boolean {
        return try {
            val root = rootInActiveWindow ?: return false
            val node = findNodeByText(root, text)
            node?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
            node != null
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 查找文本节点
     */
    private fun findNodeByText(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        if (node.text?.toString()?.contains(text, ignoreCase = true) == true) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeByText(child, text)
            if (result != null) return result
        }
        
        return null
    }

    /**
     * 输入文本
     */
    fun inputText(text: String): Boolean {
        return try {
            val root = rootInActiveWindow ?: return false
            val node = findEditText(root)
            node?.performAction(
                android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT,
                android.os.Bundle().apply {
                    putCharSequence(
                        android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        text
                    )
                }
            )
            node != null
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 查找输入框
     */
    private fun findEditText(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val className = node.className?.toString() ?: ""
        if (className.contains("EditText", ignoreCase = true)) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findEditText(child)
            if (result != null) return result
        }
        
        return null
    }

    /**
     * 返回操作
     */
    fun performBack(): Boolean {
        return try {
            performGlobalAction(GLOBAL_ACTION_BACK)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 回到桌面
     */
    fun performHome(): Boolean {
        return try {
            performGlobalAction(GLOBAL_ACTION_HOME)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 打开应用
     */
    fun launchApp(packageName: String): Boolean {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
