package com.aiyifan.app.feature.video

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ImageButton
import androidx.core.app.NotificationCompat
import androidx.media3.ui.PlayerView
import com.aiyifan.app.R
import com.aiyifan.app.core.data.AppGraph

class FloatingPlayerService : Service() {
    private val windowManager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }
    private val controllerHolder = lazy { AppGraph.videoPlaybackController }
    private val controller: VideoPlaybackController
        get() = controllerHolder.value

    private var floatingRoot: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var isClosing = false
    private var controlsVisible = true
    private var controlsShownAtMs = 0L
    private val controlsAutoHideRunnable = Runnable { hideControlsWhenIdle() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        if (floatingRoot == null) {
            showFloatingPlayer()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        closeFloatingPlayer(stopService = false)
        super.onDestroy()
    }

    private fun showFloatingPlayer() {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildMediaNotification())

        val root = LayoutInflater.from(this).inflate(R.layout.view_floating_player, null)
        val params = createLayoutParams()
        floatingRoot = root
        layoutParams = params
        bindControls(root)
        showControls(root)

        try {
            controller.attach(root.findViewById<PlayerView>(R.id.floatingPlayerView))
            windowManager.addView(root, params)
        } catch (exception: RuntimeException) {
            controller.detach()
            floatingRoot = null
            layoutParams = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun bindControls(root: View) {
        root.findViewById<ImageButton>(R.id.floatingCloseButton).setOnClickListener {
            closeFloatingPlayer(stopService = true)
        }

        val playPauseButton = root.findViewById<ImageButton>(R.id.floatingPlayPauseButton)
        playPauseButton.setOnClickListener {
            controller.togglePlayPause()
            playPauseButton.setImageResource(if (controller.isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
            playPauseButton.contentDescription = if (controller.isPlaying) "暂停播放" else "继续播放"
        }

        val dragTouchListener = DragTouchListener()
        root.setOnTouchListener(dragTouchListener)
        root.findViewById<PlayerView>(R.id.floatingPlayerView).setOnTouchListener(dragTouchListener)
        root.findViewById<View>(R.id.floatingResizeHandle).setOnTouchListener(ResizeTouchListener())
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        val defaultWidth = dpToPx(DEFAULT_WIDTH_DP)
        val size = FloatingWindowSizePolicy.resize(
            requestedWidth = defaultWidth,
            minWidth = dpToPx(MIN_WIDTH_DP),
            maxWidth = dpToPx(MAX_WIDTH_DP),
        )
        return WindowManager.LayoutParams(
            size.width,
            size.height,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dpToPx(DEFAULT_MARGIN_DP)
            y = dpToPx(DEFAULT_MARGIN_DP)
        }
    }

    private fun closeFloatingPlayer(stopService: Boolean) {
        if (isClosing) return
        isClosing = true

        val root = floatingRoot
        floatingRoot = null
        layoutParams = null
        root?.removeCallbacks(controlsAutoHideRunnable)
        if (root != null) {
            runCatching { windowManager.removeView(root) }
        }
        if (controllerHolder.isInitialized()) {
            FloatingPlayerRecovery.record(this, controller.currentPositionMs)
            controller.saveHistory()
            controller.release()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (stopService) stopSelf()
    }

    private fun buildMediaNotification() = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle("爱壹帆视频播放中")
        .setContentText("悬浮播放器正在运行")
        .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
        .setOngoing(true)
        .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "悬浮视频播放",
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun overlayWindowType(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
        @Suppress("DEPRECATION")
        WindowManager.LayoutParams.TYPE_PHONE
    }

    private fun dpToPx(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun showControls(root: View) {
        controlsVisible = true
        controlsShownAtMs = SystemClock.elapsedRealtime()
        root.findViewById<View>(R.id.floatingCloseButton).visibility = View.VISIBLE
        root.findViewById<View>(R.id.floatingPlayPauseButton).visibility = View.VISIBLE
        root.findViewById<View>(R.id.floatingResizeHandle).visibility = View.VISIBLE
        root.removeCallbacks(controlsAutoHideRunnable)
        root.postDelayed(controlsAutoHideRunnable, CONTROLS_AUTO_HIDE_DELAY_MS)
    }

    private fun hideControlsWhenIdle() {
        val root = floatingRoot ?: return
        val nowMs = SystemClock.elapsedRealtime()
        if (!FloatingControlsVisibilityPolicy.shouldAutoHide(
                controlsVisible = controlsVisible,
                shownAtMs = controlsShownAtMs,
                nowMs = nowMs,
                delayMs = CONTROLS_AUTO_HIDE_DELAY_MS,
            )
        ) {
            root.postDelayed(
                controlsAutoHideRunnable,
                (CONTROLS_AUTO_HIDE_DELAY_MS - (nowMs - controlsShownAtMs)).coerceAtLeast(0L),
            )
            return
        }
        controlsVisible = false
        root.findViewById<View>(R.id.floatingCloseButton).visibility = View.GONE
        root.findViewById<View>(R.id.floatingPlayPauseButton).visibility = View.GONE
        root.findViewById<View>(R.id.floatingResizeHandle).visibility = View.GONE
    }

    private fun toggleControls(root: View) {
        if (FloatingControlsVisibilityPolicy.togglesToVisible(controlsVisible)) {
            showControls(root)
        } else {
            root.removeCallbacks(controlsAutoHideRunnable)
            controlsVisible = false
            root.findViewById<View>(R.id.floatingCloseButton).visibility = View.GONE
            root.findViewById<View>(R.id.floatingPlayPauseButton).visibility = View.GONE
            root.findViewById<View>(R.id.floatingResizeHandle).visibility = View.GONE
        }
    }

    private fun displayMetrics(): DisplayMetrics = DisplayMetrics().also { metrics ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            metrics.widthPixels = bounds.width()
            metrics.heightPixels = bounds.height()
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
        }
    }

    private fun clampPosition(params: WindowManager.LayoutParams): FloatingWindowPosition {
        val displayMetrics = displayMetrics()
        return FloatingWindowPositionPolicy.clampToDisplay(
            x = params.x,
            y = params.y,
            windowWidth = params.width,
            windowHeight = params.height,
            displayWidth = displayMetrics.widthPixels,
            displayHeight = displayMetrics.heightPixels,
        )
    }

    private fun updateLayout(params: WindowManager.LayoutParams) {
        floatingRoot?.let { windowManager.updateViewLayout(it, params) }
    }

    private inner class DragTouchListener : View.OnTouchListener {
        private var downRawX = 0f
        private var downRawY = 0f
        private var initialX = 0
        private var initialY = 0
        private var isDragging = false

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            val params = layoutParams ?: return false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    initialX = params.x
                    initialY = params.y
                    isDragging = false
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - downRawX
                    val deltaY = event.rawY - downRawY
                    if (!isDragging && maxOf(kotlin.math.abs(deltaX), kotlin.math.abs(deltaY)) >= ViewConfiguration.get(this@FloatingPlayerService).scaledTouchSlop) {
                        isDragging = true
                    }
                    if (!isDragging) return true
                    params.x = initialX + deltaX.toInt()
                    params.y = initialY + deltaY.toInt()
                    clampPosition(params).also {
                        params.x = it.x
                        params.y = it.y
                    }
                    updateLayout(params)
                    return true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isDragging) {
                        val displayPosition = clampPosition(params)
                        val displayMetrics = displayMetrics()
                        FloatingWindowPositionPolicy.snapToNearestHorizontalEdge(
                            x = displayPosition.x,
                            y = displayPosition.y,
                            windowWidth = params.width,
                            windowHeight = params.height,
                            displayWidth = displayMetrics.widthPixels,
                            displayHeight = displayMetrics.heightPixels,
                        ).also {
                            params.x = it.x
                            params.y = it.y
                        }
                        updateLayout(params)
                    } else if (event.actionMasked == MotionEvent.ACTION_UP) {
                        view.performClick()
                        floatingRoot?.let(::toggleControls)
                    }
                    return true
                }
            }
            return false
        }
    }

    private inner class ResizeTouchListener : View.OnTouchListener {
        private var downRawX = 0f
        private var initialWidth = 0

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            val params = layoutParams ?: return false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    initialWidth = params.width
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    val size = FloatingWindowSizePolicy.resize(
                        requestedWidth = initialWidth + (event.rawX - downRawX).toInt(),
                        minWidth = dpToPx(MIN_WIDTH_DP),
                        maxWidth = dpToPx(MAX_WIDTH_DP),
                    )
                    params.width = size.width
                    params.height = size.height
                    clampPosition(params).also {
                        params.x = it.x
                        params.y = it.y
                    }
                    updateLayout(params)
                    return true
                }
            }
            return false
        }
    }

    companion object {
        const val NOTIFICATION_ID = 4101
        private const val NOTIFICATION_CHANNEL_ID = "floating_video_playback"
        private const val DEFAULT_WIDTH_DP = 320
        private const val MIN_WIDTH_DP = 240
        private const val MAX_WIDTH_DP = 480
        private const val DEFAULT_MARGIN_DP = 16
        private const val CONTROLS_AUTO_HIDE_DELAY_MS = 2_500L

        fun intent(context: Context): Intent = Intent(context, FloatingPlayerService::class.java)
    }
}
