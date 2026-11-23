package com.karasu256.mcchatnative.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.karasu256.mcchatnative.R

class ConnectionService : Service() {
    override fun onCreate() {
        super.onCreate()
        ConnectionManager.start(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                ChannelId,
                getString(R.string.connection_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        val notification = Notification.Builder(this, ChannelId)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(getString(R.string.connection_notification_title))
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NotificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NotificationId, notification)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private companion object {
        const val ChannelId = "connection"
        const val NotificationId = 1
    }
}
