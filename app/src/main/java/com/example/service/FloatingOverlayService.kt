package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.FbCommentLoaderApp
import com.example.MainActivity
import com.example.R
import com.example.engine.CrawlerStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FloatingOverlayService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var stateObservationJob: Job? = null

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private lateinit var layoutParams: WindowManager.LayoutParams

    private var isMinimized = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceWithNotification()

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        setupFloatingView()
        observeCrawlerState()
    }

    private fun startForegroundServiceWithNotification() {
        val channelId = "overlay_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Floating Overlay Controller",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls the floating comment extractor"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("محمل تعليقات فيسبوك")
            .setContentText("النافذة العائمة قيد التشغيل فوق الشاشة")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)
    }

    private fun setupFloatingView() {
        val inflater = LayoutInflater.from(this)
        floatingView = inflater.inflate(R.layout.layout_floating_overlay, null)

        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 200
        }

        windowManager.addView(floatingView, layoutParams)

        setupInteractions()
    }

    private fun setupInteractions() {
        val view = floatingView ?: return
        val headerBar = view.findViewById<LinearLayout>(R.id.header_bar)
        val expandedBody = view.findViewById<LinearLayout>(R.id.layout_expanded_body)
        val collapsedPill = view.findViewById<LinearLayout>(R.id.layout_collapsed_pill)
        val btnMinimize = view.findViewById<TextView>(R.id.btn_minimize)
        val btnClose = view.findViewById<TextView>(R.id.btn_close)
        val btnPlay = view.findViewById<Button>(R.id.btn_play)
        val btnStop = view.findViewById<Button>(R.id.btn_stop)
        val btnOpenApp = view.findViewById<ImageButton>(R.id.btn_open_app)

        // Dragging logic
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        val touchListener = View.OnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    try {
                        windowManager.updateViewLayout(floatingView, layoutParams)
                    } catch (_: Exception) {}
                    true
                }
                else -> false
            }
        }

        headerBar.setOnTouchListener(touchListener)
        collapsedPill.setOnTouchListener(touchListener)

        // Minimize / Restore toggle
        val toggleMinimize = {
            isMinimized = !isMinimized
            if (isMinimized) {
                expandedBody.visibility = View.GONE
                collapsedPill.visibility = View.VISIBLE
                btnMinimize.text = "＋"
            } else {
                expandedBody.visibility = View.VISIBLE
                collapsedPill.visibility = View.GONE
                btnMinimize.text = "━"
            }
            try {
                windowManager.updateViewLayout(floatingView, layoutParams)
            } catch (_: Exception) {}
        }

        btnMinimize.setOnClickListener { toggleMinimize() }
        collapsedPill.setOnClickListener { toggleMinimize() }

        // Close button
        btnClose.setOnClickListener {
            stopSelf()
        }

        val controller = FbCommentLoaderApp.instance.extractionController

        // Play button
        btnPlay.setOnClickListener {
            controller.startExtraction()
        }

        // Stop button
        btnStop.setOnClickListener {
            controller.stopExtraction()
        }

        // Open App button
        btnOpenApp.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
        }
    }

    private fun observeCrawlerState() {
        val controller = FbCommentLoaderApp.instance.extractionController
        stateObservationJob = serviceScope.launch {
            controller.state.collectLatest { state ->
                val view = floatingView ?: return@collectLatest
                val tvCommentCounter = view.findViewById<TextView>(R.id.tv_comment_counter)
                val tvReplyCounter = view.findViewById<TextView>(R.id.tv_reply_counter)
                val tvStatusMsg = view.findViewById<TextView>(R.id.tv_status_msg)
                val tvLastAuthor = view.findViewById<TextView>(R.id.tv_last_author)
                val tvLastText = view.findViewById<TextView>(R.id.tv_last_text)
                val tvCollapsedStats = view.findViewById<TextView>(R.id.tv_collapsed_stats)
                val btnPlay = view.findViewById<Button>(R.id.btn_play)
                val btnStop = view.findViewById<Button>(R.id.btn_stop)

                tvCommentCounter.text = state.commentsCount.toString()
                tvReplyCounter.text = state.repliesCount.toString()
                tvStatusMsg.text = state.statusMessage

                if (state.lastDetectedAuthor.isNotBlank()) {
                    tvLastAuthor.text = "${state.lastDetectedAuthor}:"
                    tvLastText.text = "\"${state.lastDetectedText}\""
                } else {
                    tvLastAuthor.text = "لا توجد تعليقات جديدة"
                    tvLastText.text = "اضغط تشغيل للبدء بالجمع"
                }

                tvCollapsedStats.text = "💬 ${state.commentsCount} | ↳ ${state.repliesCount}"

                when (state.status) {
                    CrawlerStatus.RUNNING -> {
                        btnPlay.isEnabled = false
                        btnPlay.alpha = 0.5f
                        btnStop.isEnabled = true
                        btnStop.alpha = 1.0f
                    }
                    CrawlerStatus.IDLE, CrawlerStatus.PAUSED, CrawlerStatus.FINISHED -> {
                        btnPlay.isEnabled = true
                        btnPlay.alpha = 1.0f
                        btnStop.isEnabled = false
                        btnStop.alpha = 0.5f
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stateObservationJob?.cancel()
        serviceScope.cancel()

        if (floatingView != null) {
            try {
                windowManager.removeView(floatingView)
            } catch (e: Exception) {
                // View may already have been removed
            }
            floatingView = null
        }
    }
}
