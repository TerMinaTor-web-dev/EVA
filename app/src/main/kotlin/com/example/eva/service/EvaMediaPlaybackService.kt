package com.example.eva.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.example.eva.MainActivity
import com.example.eva.R
import com.example.eva.ui.components.EvaWebViewPool

/**
 * Enterprise Background Audio & Video Streaming Engine.
 * Integrates natively with Android MediaSessionCompat and androidx.media NotificationCompat.MediaStyle.
 * 
 * Renders the official Android Quick-Settings System Media Card (Mini-Player) with
 * playback controls, waveform seekbar, Bluetooth headphone keys, and lock-screen widget.
 */
class EvaMediaPlaybackService : Service() {

    companion object {
        const val CHANNEL_ID = "eva_media_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.eva.action.START_MEDIA"
        const val ACTION_STOP = "com.example.eva.action.STOP_MEDIA"
        const val ACTION_PLAY_PAUSE = "com.example.eva.action.PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.eva.action.NEXT"
        const val ACTION_PREVIOUS = "com.example.eva.action.PREVIOUS"
        const val ACTION_UPDATE_TRACK = "com.example.eva.action.UPDATE_TRACK"

        const val EXTRA_TITLE = "extra_media_title"
        const val EXTRA_SERVICE_NAME = "extra_service_name"
        const val EXTRA_SERVICE_ID = "extra_service_id"

        var activeServiceId: String = "ytmusic"
            private set

        fun startPlayback(context: Context, serviceId: String, serviceName: String, title: String) {
            activeServiceId = serviceId
            val intent = Intent(context, EvaMediaPlaybackService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SERVICE_ID, serviceId)
                putExtra(EXTRA_SERVICE_NAME, serviceName)
                putExtra(EXTRA_TITLE, title)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {}
        }

        fun updateTrack(context: Context, serviceId: String, artist: String, title: String) {
            activeServiceId = serviceId
            val intent = Intent(context, EvaMediaPlaybackService::class.java).apply {
                action = ACTION_UPDATE_TRACK
                putExtra(EXTRA_SERVICE_ID, serviceId)
                putExtra(EXTRA_SERVICE_NAME, artist)
                putExtra(EXTRA_TITLE, title)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {}
        }

        fun stopPlayback(context: Context) {
            val intent = Intent(context, EvaMediaPlaybackService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var mediaSession: MediaSessionCompat? = null
    private var currentTitle: String = "EVA Player Active"
    private var currentServiceName: String = "Background Streaming"
    private var isPlaying: Boolean = true

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "EVA::BackgroundAudioWakeLock"
        )

        // Initialize Native Android MediaSession for System Quick Settings Card
        setupMediaSession()
    }

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "EvaMediaSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    isPlaying = true
                    updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                    EvaWebViewPool.togglePlayPause(activeServiceId)
                    refreshNotification()
                }

                override fun onPause() {
                    isPlaying = false
                    updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                    EvaWebViewPool.togglePlayPause(activeServiceId)
                    refreshNotification()
                }

                override fun onSkipToNext() {
                    EvaWebViewPool.skipNext(activeServiceId)
                }

                override fun onSkipToPrevious() {
                    EvaWebViewPool.skipPrevious(activeServiceId)
                }

                override fun onStop() {
                    stopServiceGracefully()
                }
            })

            isActive = true
        }
    }

    private fun updatePlaybackState(state: Int) {
        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_STOP
            )
            .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f)

        mediaSession?.setPlaybackState(stateBuilder.build())
    }

    private fun updateMetadata(serviceName: String, title: String) {
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, serviceName)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "EVA One-For-All")
            .build()
        mediaSession?.setMetadata(metadata)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopServiceGracefully()
                return START_NOT_STICKY
            }
            ACTION_PLAY_PAUSE -> {
                isPlaying = !isPlaying
                val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
                updatePlaybackState(state)
                EvaWebViewPool.togglePlayPause(activeServiceId)
                refreshNotification()
                return START_STICKY
            }
            ACTION_NEXT -> {
                EvaWebViewPool.skipNext(activeServiceId)
                return START_STICKY
            }
            ACTION_PREVIOUS -> {
                EvaWebViewPool.skipPrevious(activeServiceId)
                return START_STICKY
            }
            ACTION_UPDATE_TRACK -> {
                val serviceName = intent.getStringExtra(EXTRA_SERVICE_NAME) ?: currentServiceName
                val title = intent.getStringExtra(EXTRA_TITLE) ?: currentTitle
                val serviceId = intent.getStringExtra(EXTRA_SERVICE_ID) ?: activeServiceId
                activeServiceId = serviceId

                currentServiceName = serviceName
                currentTitle = title
                isPlaying = true

                acquireWakeLock()
                updateMetadata(serviceName, title)
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                refreshNotification()
            }
            ACTION_START -> {
                val serviceName = intent.getStringExtra(EXTRA_SERVICE_NAME) ?: "EVA Media"
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Background Stream Active"
                val serviceId = intent.getStringExtra(EXTRA_SERVICE_ID) ?: activeServiceId
                activeServiceId = serviceId

                currentServiceName = serviceName
                currentTitle = title
                isPlaying = true

                acquireWakeLock()
                updateMetadata(serviceName, title)
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)

                val notification = buildNotification(serviceName, title, isPlaying)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
        }
        return START_STICKY
    }

    private fun refreshNotification() {
        val notification = buildNotification(currentServiceName, currentTitle, isPlaying)
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(serviceName: String, title: String, playing: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Notification Action: Play/Pause
        val playPauseIntent = Intent(this, EvaMediaPlaybackService::class.java).apply {
            action = ACTION_PLAY_PAUSE
        }
        val playPausePendingIntent = PendingIntent.getService(
            this,
            1,
            playPauseIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Notification Action: Previous
        val prevIntent = Intent(this, EvaMediaPlaybackService::class.java).apply {
            action = ACTION_PREVIOUS
        }
        val prevPendingIntent = PendingIntent.getService(
            this,
            2,
            prevIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Notification Action: Next
        val nextIntent = Intent(this, EvaMediaPlaybackService::class.java).apply {
            action = ACTION_NEXT
        }
        val nextPendingIntent = PendingIntent.getService(
            this,
            3,
            nextIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Stop / Cancel Action
        val stopIntent = Intent(this, EvaMediaPlaybackService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            4,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // System Media Style Notification - Powers Android Quick Settings Mini-Player
        val mediaStyle = MediaStyle()
            .setMediaSession(mediaSession?.sessionToken)
            .setShowActionsInCompactView(0, 1, 2)
            .setShowCancelButton(true)
            .setCancelButtonIntent(stopPendingIntent)

        val playPauseIcon = if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseText = if (playing) "Pause" else "Play"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setStyle(mediaStyle)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(serviceName)
            .setSubText("EVA Player")
            .setContentIntent(openPendingIntent)
            .setOngoing(playing)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPendingIntent)
            .addAction(playPauseIcon, playPauseText, playPausePendingIntent)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "EVA Background Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps audio and media streams active in the background"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == false) {
            wakeLock?.acquire(3 * 60 * 60 * 1000L /* 3 hours max */)
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    private fun stopServiceGracefully() {
        releaseWakeLock()
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopServiceGracefully()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
