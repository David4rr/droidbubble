package com.droidbubble.app.model

import android.app.PendingIntent
import android.graphics.Bitmap

data class MediaInfo(
    val packageName: String,
    val title: String,
    val artist: String,
    val albumArt: Bitmap? = null,
    val isPlaying: Boolean = false,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L
)

data class NotificationAction(
    val title: String,
    val actionIntent: PendingIntent?
)

data class NotificationInfo(
    val key: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val iconBitmap: Bitmap? = null,
    val contentIntent: PendingIntent? = null,
    val actions: List<NotificationAction> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class ScreenRecordingState(
    val isRecording: Boolean = false,
    val durationSeconds: Long = 0L,
    val isPaused: Boolean = false
)

enum class IslandPriority {
    SCREEN_RECORDING,
    POP_NOTIFICATION,
    MEDIA,
    IDLE
}
