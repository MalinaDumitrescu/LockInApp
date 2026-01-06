package com.example.lockinapp.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object VpnNotification {
    private const val CH_ID = "lockinapp_vpn"

    fun build(ctx: Context): Notification {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CH_ID, "LockInApp VPN", NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(ch)
        }
        return NotificationCompat.Builder(ctx, CH_ID)
            .setSmallIcon(ctx.applicationInfo.icon)
            .setContentTitle("LockInApp")
            .setContentText("VPN is running")
            .setOngoing(true)
            .build()
    }
}
