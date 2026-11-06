package com.example.eva.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.eva.MainActivity
import com.example.eva.R

/**
 * Ambient Persistent Super-App Controller for EVA.
 *
 * When an official native APK (Instagram, YouTube, etc.) is launched from EVA,
 * this ambient notification remains active in the status shade to provide:
 * 1. 1-tap "Return to EVA Hub" navigation.
 * 2. Visual indicator that the native app is managed under EVA's hybrid cloud ecosystem.
 */
object EvaAmbientNotificationManager {

    const val CHANNEL_ID = "eva_ambient_controller"
    const val NOTIFICATION_ID = 2026

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "EVA Super-App Ambient Hub",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ambient persistent controller for running native apps"
                setShowBadge(false)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun showActiveAppNotification(context: Context, serviceName: String, packageName: String) {
        try {
            createNotificationChannel(context)

            val returnIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val returnPendingIntent = PendingIntent.getActivity(
                context,
                0,
                returnIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("EVA Super-App • $serviceName Active")
                .setContentText("Running at 100% native hardware speed. Tap to return to EVA.")
                .setContentIntent(returnPendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(
                    android.R.drawable.ic_menu_revert,
                    "Return to EVA",
                    returnPendingIntent
                )
                .build()

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.notify(NOTIFICATION_ID, notification)
        } catch (_: Exception) {
            // Notification dispatch defense
        }
    }

    fun dismissNotification(context: Context) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.cancel(NOTIFICATION_ID)
        } catch (_: Exception) {}
    }
}
