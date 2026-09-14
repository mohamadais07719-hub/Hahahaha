package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.FbCommentLoaderApp

class FacebookCommentAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("FBAccessibility", "Accessibility Service Connected")
        try {
            FbCommentLoaderApp.instance.extractionController.accessibilityService = this
        } catch (e: Exception) {
            Log.e("FBAccessibility", "Error setting accessibility service reference", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            if (FbCommentLoaderApp.instance.extractionController.accessibilityService == null) {
                FbCommentLoaderApp.instance.extractionController.accessibilityService = this
            }
        } catch (_: Exception) {}
    }

    override fun onInterrupt() {
        Log.w("FBAccessibility", "Accessibility Service Interrupted")
        try {
            if (FbCommentLoaderApp.instance.extractionController.accessibilityService == this) {
                FbCommentLoaderApp.instance.extractionController.accessibilityService = null
            }
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("FBAccessibility", "Accessibility Service Destroyed")
        try {
            if (FbCommentLoaderApp.instance.extractionController.accessibilityService == this) {
                FbCommentLoaderApp.instance.extractionController.accessibilityService = null
            }
        } catch (_: Exception) {}
    }
}
