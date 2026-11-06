package com.example.eva.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.example.eva.MainActivity
import com.example.eva.R
import com.example.eva.ui.components.EvaWebViewPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.net.URL

/**
 * Enterprise Background Audio & Video Streaming Engine.
 * Integrates natively with Android MediaSessionCompat and androidx.media NotificationCompat.MediaStyle.
 * 
 * Features:
 * - Animated Seeker & Squiggly Wave: Sets valid playback position, 1.0f speed, and elapsedRealtime
 *   so Android 13+ / Samsung One UI renders the rich system media card with animated progress.
 * - Dynamic Album Artwork & Material You Tinting: Downloads remote album art asynchronously and
 *   provides crisp fallback vector badges for immediate system color tinting.
 * - Multi-App Audio Focus & Concurrency Integration with EvaMediaCoordinator.
 * - Thermal & Battery Safeguard: Automatically releases CPU WakeLock and Wi-Fi High-Perf locks
 *   as soon as playback is paused or stopped, allowing CPU to enter deep sleep.
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
        const val ACTION_UPDATE_MEDIA_STATE = "com.example.eva.action.UPDATE_MEDIA_STATE"
        const val ACTION_SET_PLAYING = "com.example.eva.action.SET_PLAYING"
        const val ACTION_UPDATE_POSITION = "com.example.eva.action.UPDATE_POSITION"

        const val EXTRA_TITLE = "extra_media_title"
        const val EXTRA_SERVICE_NAME = "extra_service_name"
        const val EXTRA_SERVICE_ID = "extra_service_id"
        const val EXTRA_ARTIST = "extra_media_artist"
        const val EXTRA_ARTWORK_URL = "extra_media_artwork_url"
        const val EXTRA_IS_PLAYING = "extra_is_playing"
        const val EXTRA_DURATION_MS = "extra_duration_ms"
        const val EXTRA_POSITION_MS = "extra_position_ms"

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
            startServiceSafely(context, intent)
        }

        fun updateMediaState(
            context: Context,
            serviceId: String,
            serviceName: String,
            title: String,
            artist: String,
            artworkUrl: String,
            isPlaying: Boolean,
            durationMs: Long,
            positionMs: Long
        ) {
            activeServiceId = serviceId
            val intent = Intent(context, EvaMediaPlaybackService::class.java).apply {
                action = ACTION_UPDATE_MEDIA_STATE
                putExtra(EXTRA_SERVICE_ID, serviceId)
                putExtra(EXTRA_SERVICE_NAME, serviceName)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_ARTIST, artist)
                putExtra(EXTRA_ARTWORK_URL, artworkUrl)
                putExtra(EXTRA_IS_PLAYING, isPlaying)
                putExtra(EXTRA_DURATION_MS, durationMs)
                putExtra(EXTRA_POSITION_MS, positionMs)
            }
            startServiceSafely(context, intent)
        }

        fun setPlayingState(context: Context, isPlaying: Boolean, positionMs: Long = -1L, durationMs: Long = -1L) {
            val intent = Intent(context, EvaMediaPlaybackService::class.java).apply {
                action = ACTION_SET_PLAYING
                putExtra(EXTRA_IS_PLAYING, isPlaying)
                putExtra(EXTRA_POSITION_MS, positionMs)
                putExtra(EXTRA_DURATION_MS, durationMs)
            }
            startServiceSafely(context, intent)
        }

        fun updatePosition(context: Context, positionMs: Long, durationMs: Long) {
            val intent = Intent(context, EvaMediaPlaybackService::class.java).apply {
                action = ACTION_UPDATE_POSITION
                putExtra(EXTRA_POSITION_MS, positionMs)
                putExtra(EXTRA_DURATION_MS, durationMs)
            }
            startServiceSafely(context, intent)
        }

        fun updateTrack(context: Context, serviceId: String, artist: String, title: String) {
            activeServiceId = serviceId
            val intent = Intent(context, EvaMediaPlaybackService::class.java).apply {
                action = ACTION_UPDATE_TRACK
                putExtra(EXTRA_SERVICE_ID, serviceId)
                putExtra(EXTRA_SERVICE_NAME, artist)
                putExtra(EXTRA_TITLE, title)
            }
            startServiceSafely(context, intent)
        }

        fun stopPlayback(context: Context) {
            val intent = Intent(context, EvaMediaPlaybackService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }

        private fun startServiceSafely(context: Context, intent: Intent) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {}
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var mediaSession: MediaSessionCompat? = null

    private var currentTitle: String = "EVA Player Active"
    private var currentServiceName: String = "Background Streaming"
    private var currentArtist: String = "EVA Media"
    private var currentArtworkUrl: String = ""
    private var currentArtworkBitmap: Bitmap? = null
    private var isPlaying: Boolean = true
    private var currentDurationMs: Long = 0L
    private var currentPositionMs: Long = 0L

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var artworkDownloadJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // 1. Partial WakeLock: Keep CPU alive when screen is locked
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "EVA::BackgroundAudioWakeLock"
        )

        // 2. High-Performance WifiLock: Prevent Samsung One UI from throttling network sockets
        try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            @Suppress("DEPRECATION")
            wifiLock = wifiManager?.createWifiLock(
                WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                "EVA::MediaWifiLock"
            )
        } catch (_: Exception) {}

        // 3. Initialize Native Android MediaSession for System Quick Settings Card
        setupMediaSession()
    }

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "EvaMediaSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    isPlaying = true
                    acquireLocks()
                    updatePlaybackState(PlaybackStateCompat.STATE_PLAYING, currentPositionMs, currentDurationMs)
                    EvaWebViewPool.setPlayPauseState(activeServiceId, true)
                    refreshNotification()
                }

                override fun onPause() {
                    isPlaying = false
                    releaseLocks() // THERMAL FIX: Stop holding CPU awake when paused
                    updatePlaybackState(PlaybackStateCompat.STATE_PAUSED, currentPositionMs, currentDurationMs)
                    EvaMediaCoordinator.onUserExplicitPause(this@EvaMediaPlaybackService)
                    EvaWebViewPool.setPlayPauseState(activeServiceId, false)
                    refreshNotification()
                }

                override fun onSkipToNext() {
                    EvaWebViewPool.skipNext(activeServiceId)
                }

                override fun onSkipToPrevious() {
                    EvaWebViewPool.skipPrevious(activeServiceId)
                }

                override fun onSeekTo(pos: Long) {
                    currentPositionMs = pos
                    val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
                    updatePlaybackState(state, pos, currentDurationMs)
                    EvaWebViewPool.seekTo(activeServiceId, pos)
                }

                override fun onStop() {
                    stopServiceGracefully()
                }
            })

            isActive = true
        }
    }

    /**
     * Updates playback state with position, speed and elapsedRealtime.
     * This triggers Android 13+ / Samsung One UI animated wave progress bar.
     */
    private fun updatePlaybackState(state: Int, positionMs: Long = currentPositionMs, durationMs: Long = currentDurationMs) {
        val speed = if (state == PlaybackStateCompat.STATE_PLAYING) 1.0f else 0.0f
        val validPosition = if (positionMs >= 0) positionMs else 0L

        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SEEK_TO or
                PlaybackStateCompat.ACTION_STOP
            )
            .setState(state, validPosition, speed, SystemClock.elapsedRealtime())

        mediaSession?.setPlaybackState(stateBuilder.build())
    }

    /**
     * Updates track metadata with duration and artwork bitmap.
     * Android uses the artwork bitmap to dynamically colorize the system media notification.
     */
    private fun updateMetadata(
        serviceName: String,
        title: String,
        artist: String,
        durationMs: Long,
        artwork: Bitmap?
    ) {
        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "EVA • Super-App")
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, artist)

        if (durationMs > 0) {
            metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationMs)
        }

        val art = artwork ?: getFallbackBrandedBitmap(activeServiceId)
        if (art != null) {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, art)
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, art)
        }

        mediaSession?.setMetadata(metadataBuilder.build())
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
                if (isPlaying) {
                    acquireLocks()
                } else {
                    releaseLocks()
                    EvaMediaCoordinator.onUserExplicitPause(this)
                }
                updatePlaybackState(state, currentPositionMs, currentDurationMs)
                EvaWebViewPool.setPlayPauseState(activeServiceId, isPlaying)
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
            ACTION_SET_PLAYING -> {
                val playing = intent.getBooleanExtra(EXTRA_IS_PLAYING, true)
                val pos = intent.getLongExtra(EXTRA_POSITION_MS, -1L)
                val dur = intent.getLongExtra(EXTRA_DURATION_MS, -1L)
                if (pos >= 0) currentPositionMs = pos
                if (dur >= 0) currentDurationMs = dur

                isPlaying = playing
                if (playing) {
                    acquireLocks()
                } else {
                    releaseLocks()
                }
                val state = if (playing) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
                updatePlaybackState(state, currentPositionMs, currentDurationMs)
                refreshNotification()
            }
            ACTION_UPDATE_POSITION -> {
                val pos = intent.getLongExtra(EXTRA_POSITION_MS, currentPositionMs)
                val dur = intent.getLongExtra(EXTRA_DURATION_MS, currentDurationMs)
                currentPositionMs = pos
                if (dur > 0) currentDurationMs = dur
                if (isPlaying) {
                    updatePlaybackState(PlaybackStateCompat.STATE_PLAYING, pos, currentDurationMs)
                }
            }
            ACTION_UPDATE_MEDIA_STATE -> {
                val serviceId = intent.getStringExtra(EXTRA_SERVICE_ID) ?: activeServiceId
                val serviceName = intent.getStringExtra(EXTRA_SERVICE_NAME) ?: currentServiceName
                val title = intent.getStringExtra(EXTRA_TITLE) ?: currentTitle
                val artist = intent.getStringExtra(EXTRA_ARTIST) ?: currentArtist
                val artworkUrl = intent.getStringExtra(EXTRA_ARTWORK_URL) ?: ""
                val playing = intent.getBooleanExtra(EXTRA_IS_PLAYING, true)
                val dur = intent.getLongExtra(EXTRA_DURATION_MS, 0L)
                val pos = intent.getLongExtra(EXTRA_POSITION_MS, 0L)

                activeServiceId = serviceId
                currentServiceName = serviceName
                currentTitle = title
                currentArtist = artist
                isPlaying = playing
                if (dur > 0) currentDurationMs = dur
                if (pos >= 0) currentPositionMs = pos

                if (playing) {
                    acquireLocks()
                } else {
                    releaseLocks()
                }

                if (artworkUrl.isNotBlank() && artworkUrl != currentArtworkUrl) {
                    currentArtworkUrl = artworkUrl
                    downloadArtworkAsync(artworkUrl)
                }

                updateMetadata(serviceName, title, artist, currentDurationMs, currentArtworkBitmap)
                updatePlaybackState(if (playing) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED, currentPositionMs, currentDurationMs)
                refreshNotification()
            }
            ACTION_UPDATE_TRACK -> {
                val serviceName = intent.getStringExtra(EXTRA_SERVICE_NAME) ?: currentServiceName
                val title = intent.getStringExtra(EXTRA_TITLE) ?: currentTitle
                val serviceId = intent.getStringExtra(EXTRA_SERVICE_ID) ?: activeServiceId
                activeServiceId = serviceId

                currentServiceName = serviceName
                currentTitle = title
                isPlaying = true

                acquireLocks()
                updateMetadata(serviceName, title, currentArtist, currentDurationMs, currentArtworkBitmap)
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING, currentPositionMs, currentDurationMs)
                refreshNotification()
            }
            ACTION_START -> {
                val serviceName = intent.getStringExtra(EXTRA_SERVICE_NAME) ?: "EVA Media"
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Background Stream Active"
                val serviceId = intent.getStringExtra(EXTRA_SERVICE_ID) ?: activeServiceId
                activeServiceId = serviceId

                currentServiceName = serviceName
                currentTitle = title
                currentArtist = serviceName
                isPlaying = true

                acquireLocks()
                updateMetadata(serviceName, title, currentArtist, currentDurationMs, currentArtworkBitmap)
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING, currentPositionMs, currentDurationMs)

                val notification = buildNotification(serviceName, title, currentArtist, isPlaying)

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

    private fun downloadArtworkAsync(urlStr: String) {
        artworkDownloadJob?.cancel()
        artworkDownloadJob = serviceScope.launch(Dispatchers.IO) {
            try {
                val url = URL(urlStr)
                val connection = url.openConnection()
                connection.connectTimeout = 4000
                connection.readTimeout = 4000
                val stream = connection.getInputStream()
                val bitmap = BitmapFactory.decodeStream(stream)
                stream.close()
                if (bitmap != null) {
                    currentArtworkBitmap = bitmap
                    updateMetadata(currentServiceName, currentTitle, currentArtist, currentDurationMs, bitmap)
                    refreshNotification()
                }
            } catch (_: Exception) {}
        }
    }

    private fun refreshNotification() {
        val notification = buildNotification(currentServiceName, currentTitle, currentArtist, isPlaying)
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(serviceName: String, title: String, artist: String, playing: Boolean): Notification {
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

        // Stop Action
        val stopIntent = Intent(this, EvaMediaPlaybackService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            4,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val mediaStyle = MediaStyle()
            .setMediaSession(mediaSession?.sessionToken)
            .setShowActionsInCompactView(0, 1, 2)
            .setShowCancelButton(true)
            .setCancelButtonIntent(stopPendingIntent)

        val playPauseIcon = if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseText = if (playing) "Pause" else "Play"

        val largeIcon = currentArtworkBitmap ?: getFallbackBrandedBitmap(activeServiceId)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setStyle(mediaStyle)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(artist)
            .setSubText(serviceName)
            .setContentIntent(openPendingIntent)
            .setOngoing(playing)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPendingIntent)
            .addAction(playPauseIcon, playPauseText, playPausePendingIntent)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        return builder.build()
    }

    private fun getFallbackBrandedBitmap(serviceId: String): Bitmap? {
        return try {
            val size = 256
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            val color = when (serviceId.lowercase()) {
                "ytmusic", "youtube" -> Color.parseColor("#FF0000")
                "spotify" -> Color.parseColor("#1DB954")
                "instagram" -> Color.parseColor("#E1306C")
                "facebook" -> Color.parseColor("#1877F2")
                "twitter" -> Color.parseColor("#1DA1F2")
                "reddit" -> Color.parseColor("#FF4500")
                else -> Color.parseColor("#00F0FF")
            }

            paint.color = color
            canvas.drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), 48f, 48f, paint)

            paint.color = Color.WHITE
            paint.textSize = 96f
            paint.textAlign = Paint.Align.CENTER
            paint.isFakeBoldText = true

            val initial = when (serviceId.lowercase()) {
                "ytmusic" -> "YM"
                "spotify" -> "SP"
                "youtube" -> "YT"
                "instagram" -> "IG"
                "reddit" -> "RD"
                else -> "EV"
            }

            val yPos = (canvas.height / 2 - (paint.descent() + paint.ascent()) / 2)
            canvas.drawText(initial, canvas.width / 2f, yPos, paint)

            bitmap
        } catch (_: Exception) {
            null
        }
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

    private fun acquireLocks() {
        try {
            if (wakeLock?.isHeld != true) {
                wakeLock?.acquire(6 * 60 * 60 * 1000L)
            }
            if (wifiLock?.isHeld != true) {
                wifiLock?.acquire()
            }
        } catch (_: Exception) {}
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
            if (wifiLock?.isHeld == true) wifiLock?.release()
        } catch (_: Exception) {}
    }

    private fun stopServiceGracefully() {
        releaseLocks()
        artworkDownloadJob?.cancel()
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
