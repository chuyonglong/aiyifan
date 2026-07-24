package com.aiyifan.app.feature.video

import android.app.NotificationChannel
import android.app.NotificationManager
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

        root.findViewById<View>(R.id.floatingDragHandle).setOnTouchListener(DragTouchListener())
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
        if (root != null) {
            runCatching { windowManager.removeView(root) }
        }
        if (controllerHolder.isInitialized()) {
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

    private inner class DragTouchListener : View.OnTouchListener {
        private var downRawX = 0f
        private var downRawY = 0f
        private var initialX = 0
        private var initialY = 0

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            val params = layoutParams ?: return false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    initialX = params.x
                    initialY = params.y
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - downRawX).toInt()
                    params.y = initialY + (event.rawY - downRawY).toInt()
                    floatingRoot?.let { windowManager.updateViewLayout(it, params) }
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
                    floatingRoot?.let { windowManager.updateViewLayout(it, params) }
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

        fun intent(context: Context): Intent = Intent(context, FloatingPlayerService::class.java)
    }
}
