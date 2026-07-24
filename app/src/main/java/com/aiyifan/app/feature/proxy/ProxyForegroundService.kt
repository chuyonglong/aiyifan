package com.aiyifan.app.feature.proxy

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.aiyifan.app.R
import com.aiyifan.app.core.data.AppGraph

class ProxyForegroundService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        startForeground(
            NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Network proxy connected")
                .setContentText("Aiyifan traffic is routed through the selected node")
                .setOngoing(true)
                .build(),
        )
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (AppGraph.proxyManager.activeEndpoint != null) {
            AppGraph.proxyManager.disconnect()
        }
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Network proxy", NotificationManager.IMPORTANCE_LOW),
        )
    }

    companion object {
        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, ProxyForegroundService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ProxyForegroundService::class.java))
        }

        private const val CHANNEL_ID = "proxy_connection"
        private const val NOTIFICATION_ID = 4102
    }
}
