package com.example.engine

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.CommentRepository
import com.example.data.model.CommentItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExtractionController(private val repository: CommentRepository) {

    companion object {
        private const val TAG = "ExtractionController"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var extractionJob: Job? = null

    private val crawlerEngine = FacebookCrawlerEngine()

    private val _state = MutableStateFlow(CrawlerState())
    val state: StateFlow<CrawlerState> = _state.asStateFlow()

    // Deduplication cache
    private val seenSignatures = mutableSetOf<String>()

    // Reference to connected AccessibilityService
    @Volatile
    var accessibilityService: AccessibilityService? = null

    // Track consecutive idle scrolls to detect end of post comments
    private var consecutiveIdleCycles = 0

    fun updateScrollDelay(delayMs: Long) {
        _state.update { it.copy(scrollDelayMs = delayMs) }
    }

    fun toggleAutoExpandReplies(enabled: Boolean) {
        _state.update { it.copy(isAutoExpandReplies = enabled) }
    }

    fun startExtraction(customTitle: String? = null) {
        if (_state.value.status == CrawlerStatus.RUNNING) return

        extractionJob?.cancel()
        crawlerEngine.reset()
        consecutiveIdleCycles = 0

        scope.launch {
            val title = customTitle ?: run {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                "منشور فيسبوك - " + dateFormat.format(Date())
            }

            val sessionId = repository.createSession(title)
            seenSignatures.clear()

            _state.update {
                it.copy(
                    status = CrawlerStatus.RUNNING,
                    currentSessionId = sessionId,
                    commentsCount = 0,
                    repliesCount = 0,
                    lastDetectedAuthor = "",
                    lastDetectedText = "",
                    statusMessage = "جاري البحث عن نافذة فيسبوك والتعليقات..."
                )
            }

            startExtractionLoop(sessionId)
        }
    }

    private fun startExtractionLoop(sessionId: Long) {
        extractionJob = scope.launch {
            while (isActive && _state.value.status == CrawlerStatus.RUNNING) {
                val service = accessibilityService
                if (service == null) {
                    _state.update {
                        it.copy(statusMessage = "بانتظار تفعيل خدمة إمكانية الوصول...")
                    }
                    delay(1500)
                    continue
                }

                val rootNode = findTargetWindowRoot(service)
                val currentState = _state.value

                if (rootNode == null) {
                    _state.update {
                        it.copy(statusMessage = "يرجى فتح تطبيق فيسبوك أو المنشور...")
                    }
                    delay(1200)
                    continue
                }

                try {
                    // Step 1: Scan and expand buttons or extract comments
                    val scanResult = crawlerEngine.analyzeAndAct(
                        service = service,
                        rootNode = rootNode,
                        sessionId = sessionId,
                        seenSignatures = seenSignatures,
                        autoExpandReplies = currentState.isAutoExpandReplies
                    )

                    var activityOccurred = false

                    // If a button was clicked (e.g. view replies or view more comments), wait for Facebook to load
                    if (scanResult.clickedButtonType != null) {
                        activityOccurred = true
                        consecutiveIdleCycles = 0
                        _state.update {
                            it.copy(statusMessage = "تم النقر على: ${scanResult.clickedButtonType} (جاري التحميل...)")
                        }
                        // Generous delay to give Facebook time to populate the reply/comment nodes
                        delay(currentState.scrollDelayMs + 300L)
                    }

                    // Process extracted comments
                    if (scanResult.newComments.isNotEmpty()) {
                        activityOccurred = true
                        consecutiveIdleCycles = 0

                        var newCommentsCount = 0
                        var newRepliesCount = 0
                        var lastAuthor = ""
                        var lastText = ""

                        for (comment in scanResult.newComments) {
                            if (seenSignatures.add(comment.signature)) {
                                repository.saveComment(comment)
                                if (comment.isReply) {
                                    newRepliesCount++
                                } else {
                                    newCommentsCount++
                                }
                                lastAuthor = comment.author
                                lastText = comment.content
                            }
                        }

                        if (newCommentsCount > 0 || newRepliesCount > 0) {
                            _state.update {
                                val updatedComments = it.commentsCount + newCommentsCount
                                val updatedReplies = it.repliesCount + newRepliesCount
                                it.copy(
                                    commentsCount = updatedComments,
                                    repliesCount = updatedReplies,
                                    lastDetectedAuthor = lastAuthor,
                                    lastDetectedText = lastText,
                                    statusMessage = "تم تسجيل +$newCommentsCount تعليق و +$newRepliesCount رد جديد"
                                )
                            }
                            repository.updateSessionCounts(
                                sessionId,
                                _state.value.commentsCount,
                                _state.value.repliesCount
                            )
                        }
                    }

                    // Step 2: Smooth scroll to discover more comments
                    if (!activityOccurred) {
                        consecutiveIdleCycles++
                        _state.update {
                            it.copy(statusMessage = "جاري التمرير للأسفل لكشف مزيد من التعليقات...")
                        }

                        val scrolled = crawlerEngine.performScroll(service, rootNode)
                        delay(currentState.scrollDelayMs)

                        // If idle for several scrolls without new content or buttons, inform the user
                        if (consecutiveIdleCycles >= 7) {
                            _state.update {
                                it.copy(
                                    statusMessage = "وصلنا لنهاية التعليقات المعروضة أو بانتظار التحميل..."
                                )
                            }
                            delay(2000)
                        }
                    } else {
                        delay(350)
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error during scan loop", e)
                    delay(1000)
                } finally {
                    try {
                        rootNode.recycle()
                    } catch (_: Exception) {}
                }
            }
        }
    }

    /**
     * Locates the Facebook window root node reliably, bypassing any floating overlay
     * or system windows that might capture active window focus.
     */
    private fun findTargetWindowRoot(service: AccessibilityService): AccessibilityNodeInfo? {
        val ourPackage = service.packageName

        // Check all interactive windows first
        try {
            val windows = service.windows
            if (!windows.isNullOrEmpty()) {
                // Priority 1: Window explicitly belonging to Facebook (Katana, Lite, Orca, etc.)
                for (window in windows) {
                    val root = window.root ?: continue
                    val pkg = root.packageName?.toString() ?: ""
                    if (pkg.contains("facebook") || pkg.contains("katana") || pkg.contains("lite")) {
                        return root
                    }
                }

                // Priority 2: Any non-system window that is not our overlay/app
                for (window in windows) {
                    val root = window.root ?: continue
                    val pkg = root.packageName?.toString() ?: ""
                    if (pkg.isNotEmpty() &&
                        pkg != ourPackage &&
                        !pkg.contains("systemui") &&
                        !pkg.contains("launcher") &&
                        !pkg.contains("inputmethod")) {
                        return root
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking service.windows: ${e.message}")
        }

        // Fallback: rootInActiveWindow if it's not our own app overlay
        val activeRoot = service.rootInActiveWindow
        if (activeRoot != null) {
            val pkg = activeRoot.packageName?.toString() ?: ""
            if (pkg != ourPackage) {
                return activeRoot
            }
        }

        return activeRoot
    }

    fun stopExtraction() {
        extractionJob?.cancel()
        extractionJob = null

        val currentId = _state.value.currentSessionId
        val finalComments = _state.value.commentsCount
        val finalReplies = _state.value.repliesCount

        scope.launch {
            if (currentId > 0) {
                repository.updateSessionCounts(currentId, finalComments, finalReplies)
            }
        }

        _state.update {
            it.copy(
                status = CrawlerStatus.PAUSED,
                statusMessage = "تم الإيقاف. الإجمالي: $finalComments تعليق، $finalReplies رد"
            )
        }
    }

    fun finishSession() {
        stopExtraction()
        _state.update {
            it.copy(
                status = CrawlerStatus.FINISHED,
                statusMessage = "تم إنهاء الجلسة بنجاح"
            )
        }
    }

    // Helper for in-app simulator test
    fun simulateMockCommentAdded(author: String, text: String, isReply: Boolean) {
        scope.launch {
            val sessionId = _state.value.currentSessionId.takeIf { it > 0 }
                ?: repository.createSession("تجربة المحاكي")

            val signature = if (isReply) {
                "REPLY|||$author|||$text|||MOCK"
            } else {
                "$author|||$text"
            }

            if (seenSignatures.add(signature)) {
                val comment = CommentItem(
                    sessionId = sessionId,
                    author = author,
                    content = text,
                    isReply = isReply,
                    signature = signature,
                    timestamp = System.currentTimeMillis()
                )
                repository.saveComment(comment)

                _state.update {
                    val updatedComments = it.commentsCount + if (!isReply) 1 else 0
                    val updatedReplies = it.repliesCount + if (isReply) 1 else 0
                    it.copy(
                        currentSessionId = sessionId,
                        commentsCount = updatedComments,
                        repliesCount = updatedReplies,
                        lastDetectedAuthor = author,
                        lastDetectedText = text,
                        statusMessage = if (isReply) "تمت إضافة رد تجريبي جديد" else "تمت إضافة تعليق تجريبي جديد"
                    )
                }
                repository.updateSessionCounts(
                    sessionId,
                    _state.value.commentsCount,
                    _state.value.repliesCount
                )
            }
        }
    }
}
