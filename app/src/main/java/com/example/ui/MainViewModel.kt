package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.FbCommentLoaderApp
import com.example.data.CommentRepository
import com.example.data.model.CommentItem
import com.example.data.model.ExtractionSession
import com.example.engine.CrawlerState
import com.example.engine.CrawlerStatus
import com.example.service.FacebookCommentAccessibilityService
import com.example.service.FloatingOverlayService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CommentRepository = (application as FbCommentLoaderApp).repository
    val extractionController = (application as FbCommentLoaderApp).extractionController

    val crawlerState: StateFlow<CrawlerState> = extractionController.state

    val allSessions: StateFlow<List<ExtractionSession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedSession = MutableStateFlow<ExtractionSession?>(null)
    val selectedSession: StateFlow<ExtractionSession?> = _selectedSession.asStateFlow()

    private val _selectedComments = MutableStateFlow<List<CommentItem>>(emptyList())
    val selectedComments: StateFlow<List<CommentItem>> = _selectedComments.asStateFlow()

    private val _isOverlayGranted = MutableStateFlow(false)
    val isOverlayGranted: StateFlow<Boolean> = _isOverlayGranted.asStateFlow()

    private val _isAccessibilityEnabled = MutableStateFlow(false)
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled.asStateFlow()

    fun checkPermissions(context: Context) {
        _isOverlayGranted.value = Settings.canDrawOverlays(context)
        _isAccessibilityEnabled.value = extractionController.accessibilityService != null ||
                isAccessibilityServiceEnabled(context, FacebookCommentAccessibilityService::class.java)
    }

    private fun isAccessibilityServiceEnabled(context: Context, service: Class<*>): Boolean {
        val expectedComponentName = "${context.packageName}/${service.name}"
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServicesSetting.contains(expectedComponentName)
    }

    fun startExtraction() {
        extractionController.startExtraction()
    }

    fun stopExtraction() {
        extractionController.stopExtraction()
    }

    fun updateScrollDelay(delayMs: Long) {
        extractionController.updateScrollDelay(delayMs)
    }

    fun toggleAutoExpandReplies(enabled: Boolean) {
        extractionController.toggleAutoExpandReplies(enabled)
    }

    fun startOverlayService(context: Context) {
        if (Settings.canDrawOverlays(context)) {
            val intent = Intent(context, FloatingOverlayService::class.java)
            context.startForegroundService(intent)
        }
    }

    fun stopOverlayService(context: Context) {
        val intent = Intent(context, FloatingOverlayService::class.java)
        context.stopService(intent)
    }

    fun selectSession(session: ExtractionSession) {
        _selectedSession.value = session
        viewModelScope.launch {
            repository.getCommentsForSession(session.id).collect {
                _selectedComments.value = it
            }
        }
    }

    fun clearSelectedSession() {
        _selectedSession.value = null
        _selectedComments.value = emptyList()
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_selectedSession.value?.id == sessionId) {
                clearSelectedSession()
            }
        }
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openOverlaySettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun launchFacebookApp(context: Context) {
        val fbIntent = context.packageManager.getLaunchIntentForPackage("com.facebook.katana")
            ?: context.packageManager.getLaunchIntentForPackage("com.facebook.lite")
            ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com"))

        fbIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(fbIntent)
    }

    fun simulateMockComment(author: String, text: String, isReply: Boolean) {
        extractionController.simulateMockCommentAdded(author, text, isReply)
    }
}
