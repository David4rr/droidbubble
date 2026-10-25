package com.droidbubble.app.service

import android.app.ActivityOptions
import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.droidbubble.app.data.SettingsRepository
import com.droidbubble.app.island.IslandStateManager
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.model.MediaInfo
import com.droidbubble.app.model.NotificationAction
import com.droidbubble.app.model.NotificationInfo
import com.droidbubble.app.model.ScreenRecordingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NotificationReceiverService : NotificationListenerService() {

    companion object {
        var instance: NotificationReceiverService? = null
            private set
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var settingsRepository: SettingsRepository
    private var currentSettings = IslandSettings()

    private var mediaSessionManager: MediaSessionManager? = null
    private var activeMediaController: MediaController? = null

    private val mediaCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateMediaInfo()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateMediaInfo()
        }

        override fun onSessionDestroyed() {
            try {
                val componentName = ComponentName(this@NotificationReceiverService, NotificationReceiverService::class.java)
                val remaining = mediaSessionManager?.getActiveSessions(componentName)
                updateActiveMediaController(remaining)
            } catch (e: Exception) {
                activeMediaController = null
                IslandStateManager.updateMedia(null)
            }
        }
    }

    private val sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        updateActiveMediaController(controllers)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        settingsRepository = SettingsRepository(this)
        serviceScope.launch {
            settingsRepository.settingsFlow.collectLatest {
                currentSettings = it
            }
        }

        IslandStateManager.onPauseMedia = {
            activeMediaController?.transportControls?.pause()
        }

        IslandStateManager.onPlayPause = {
            activeMediaController?.let { controller ->
                val state = controller.playbackState?.state
                if (state == PlaybackState.STATE_PLAYING) {
                    controller.transportControls.pause()
                } else {
                    controller.transportControls.play()
                }
            }
        }

        IslandStateManager.onSkipNext = {
            activeMediaController?.transportControls?.skipToNext()
        }

        IslandStateManager.onSkipPrevious = {
            activeMediaController?.transportControls?.skipToPrevious()
        }

        IslandStateManager.onSeekTo = { pos ->
            activeMediaController?.transportControls?.seekTo(pos)
        }

        IslandStateManager.onDismissNotification = { key ->
            runCatching { cancelNotification(key) }
        }

        IslandStateManager.onSendReply = { key, replyText ->
            val notif = IslandStateManager.activeNotification.value
            if (notif != null && notif.key == key) {
                val replyAction = notif.actions.firstOrNull { it.isReply }
                if (replyAction?.actionIntent != null && replyAction.remoteInputKey != null) {
                    val intent = Intent()
                    val bundle = Bundle()
                    bundle.putCharSequence(replyAction.remoteInputKey, replyText)
                    val remoteInput = android.app.RemoteInput.Builder(replyAction.remoteInputKey).build()
                    android.app.RemoteInput.addResultsToIntent(arrayOf(remoteInput), intent, bundle)
                    try {
                        val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            ActivityOptions.makeBasic().apply {
                                pendingIntentBackgroundActivityStartMode = ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                            }.toBundle()
                        } else null
                        if (options != null) {
                            replyAction.actionIntent.send(this@NotificationReceiverService, 0, intent, null, null, null, options)
                        } else {
                            replyAction.actionIntent.send(this@NotificationReceiverService, 0, intent)
                        }
                    } catch (e: Exception) {
                        Log.e("NotificationReceiver", "Error sending reply", e)
                    }
                }
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        try {
            mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val componentName = ComponentName(this, NotificationReceiverService::class.java)
            mediaSessionManager?.addOnActiveSessionsChangedListener(sessionsChangedListener, componentName)
            val initialControllers = mediaSessionManager?.getActiveSessions(componentName)
            updateActiveMediaController(initialControllers)
        } catch (e: Exception) {
            Log.e("NotificationReceiver", "Error registering media session listener", e)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        try {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener)
            activeMediaController?.unregisterCallback(mediaCallback)
            activeMediaController = null
        } catch (e: Exception) {
            Log.e("NotificationReceiver", "Error unregistering listener", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) {
            instance = null
        }
        recordingTimerJob?.cancel()
        serviceScope.cancel()
    }

    fun refreshMedia() {
        try {
            val componentName = ComponentName(this, NotificationReceiverService::class.java)
            val controllers = mediaSessionManager?.getActiveSessions(componentName)
            updateActiveMediaController(controllers)
        } catch (_: Exception) {
            updateMediaInfo()
        }
    }

    private fun findBestController(controllers: List<MediaController>?): MediaController? {
        if (controllers.isNullOrEmpty()) return null

        // 1. Prioritize any controller that is currently actively playing with valid metadata
        val playingWithMeta = controllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING &&
                    (!it.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).isNullOrBlank() ||
                     !it.metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE).isNullOrBlank())
        }
        if (playingWithMeta != null) return playingWithMeta

        // 2. If current active controller is still in the list and has valid metadata, keep it!
        // This ensures that when entering another app with audio that temporarily pauses our music player
        // (or an app with no song metadata), the user's music player remains active and interactive on the island!
        val current = activeMediaController
        if (current != null) {
            val stillPresent = controllers.firstOrNull { it.packageName == current.packageName }
            if (stillPresent != null) {
                val hasMeta = !stillPresent.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).isNullOrBlank() ||
                        !stillPresent.metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE).isNullOrBlank()
                if (hasMeta) {
                    return stillPresent
                }
            }
        }

        // 3. Otherwise find any controller with valid metadata (e.g. paused music player like Spotify)
        val withMeta = controllers.firstOrNull {
            val title = it.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?: it.metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            !title.isNullOrBlank()
        }
        if (withMeta != null) return withMeta

        // 4. Any controller that is currently actively playing (even without metadata)
        val playing = controllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        }
        if (playing != null) return playing

        // 5. Keep current controller if still in list
        if (current != null) {
            val stillPresent = controllers.firstOrNull { it.packageName == current.packageName }
            if (stillPresent != null) {
                return stillPresent
            }
        }

        return controllers.firstOrNull()
    }

    private fun updateActiveMediaController(controllers: List<MediaController>?) {
        val best = findBestController(controllers)
        if (best == null) {
            activeMediaController?.unregisterCallback(mediaCallback)
            activeMediaController = null
            IslandStateManager.updateMedia(null)
            return
        }

        if (best.packageName != activeMediaController?.packageName) {
            activeMediaController?.unregisterCallback(mediaCallback)
            activeMediaController = best
            activeMediaController?.registerCallback(mediaCallback)
        }
        updateMediaInfo()
    }

    private fun updateMediaInfo() {
        if (!currentSettings.showMedia) {
            IslandStateManager.updateMedia(null)
            return
        }

        val controller = activeMediaController
        if (controller == null) {
            IslandStateManager.updateMedia(null)
            return
        }

        val metadata = controller.metadata
        val playbackState = controller.playbackState

        if (metadata == null && playbackState == null) {
            IslandStateManager.updateMedia(null)
            return
        }

        val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
        val rawTitle = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)?.takeIf { it.isNotBlank() }
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM)?.takeIf { it.isNotBlank() }

        val title = rawTitle ?: if (isPlaying) "Playing Audio" else null
        if (title == null) {
            IslandStateManager.updateMedia(null)
            return
        }
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() }
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)?.takeIf { it.isNotBlank() }
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_AUTHOR)?.takeIf { it.isNotBlank() }
            ?: controller.packageName

        var artBitmap = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)

        // If bitmap is not directly available, check if artwork URI is provided
        if (artBitmap == null) {
            val uriStr = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_ART_URI)
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI)
            if (!uriStr.isNullOrBlank()) {
                artBitmap = runCatching {
                    val uri = Uri.parse(uriStr)
                    contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }.getOrNull()
            }
        }

        // Also check if an active notification from this app provides large icon art
        if (artBitmap == null) {
            artBitmap = runCatching {
                activeNotifications?.firstOrNull { it.packageName == controller.packageName }
                    ?.notification?.getLargeIcon()?.loadDrawable(this)?.let { drawableToBitmap(it) }
            }.getOrNull()
        }

        val appIconBitmap = getAppIconBitmap(controller.packageName)

        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val position = playbackState?.position ?: 0L

        IslandStateManager.updateMedia(
            MediaInfo(
                packageName = controller.packageName,
                title = title,
                artist = artist,
                albumArt = artBitmap,
                appIcon = appIconBitmap,
                sessionActivity = controller.sessionActivity,
                isPlaying = isPlaying,
                durationMs = duration,
                positionMs = position
            )
        )
    }

    private var systemRecordingKey: String? = null
    private var recordingTimerJob: Job? = null
    private var recordingStartTimeMs: Long = 0L
    private var recordingPausedAtMs: Long = 0L
    private var activeStopAction: Notification.Action? = null
    private var activePauseAction: Notification.Action? = null
    private var activeResumeAction: Notification.Action? = null

    private fun isScreenRecordingSavedNotification(sbn: StatusBarNotification, notification: Notification): Boolean {
        val extras = notification.extras
        val title = (extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "").lowercase()
        val text = (extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "").lowercase()
        val subText = (extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: "").lowercase()
        val combined = "$title $text $subText"

        return combined.contains("saved") ||
                combined.contains("finished") ||
                combined.contains("completed") ||
                combined.contains("tap to view") ||
                combined.contains("tap to share") ||
                combined.contains("share") ||
                combined.contains("deleted") ||
                combined.contains("ready")
    }

    private fun isScreenRecordingNotification(sbn: StatusBarNotification, notification: Notification): Boolean {
        // 1. If it's a "saved" / completed notification, it's NOT an active screen recording!
        if (isScreenRecordingSavedNotification(sbn, notification)) {
            return false
        }

        // 2. Active screen recording MUST be ongoing (cannot be dismissed while recording)
        val isOngoing = sbn.isOngoing ||
                (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0 ||
                (notification.flags and Notification.FLAG_NO_CLEAR) != 0
        if (!isOngoing) {
            return false
        }

        val pkg = sbn.packageName.lowercase()
        val channelId = notification.channelId?.lowercase() ?: ""
        val extras = notification.extras
        val title = (extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "").lowercase()
        val text = (extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "").lowercase()
        val subText = (extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: "").lowercase()

        val isRecordingText = title.contains("screen record") ||
                title.contains("recording screen") ||
                title.contains("screen recording") ||
                title.contains("screen recorder") ||
                title.contains("recording display") ||
                text.contains("screen record") ||
                text.contains("recording screen") ||
                text.contains("screen recording") ||
                subText.contains("recording screen")

        val isRecordingPkg = pkg.contains("screenrecorder") ||
                pkg.contains("screen_recorder") ||
                pkg.contains("screenrecord") ||
                pkg.contains("hecorat.screenrecorder") ||
                pkg == "com.android.systemui" ||
                pkg == "com.google.android.systemui"

        val isRecordingChannel = channelId.contains("screen_record") ||
                channelId.contains("screenrecord") ||
                channelId.contains("screen_capture")

        return (isRecordingChannel && (isRecordingText || isRecordingPkg)) ||
                (isRecordingPkg && isRecordingText) ||
                title.contains("recording screen") ||
                title.contains("screen recording") ||
                title.contains("screen recorder") ||
                text.contains("recording screen") ||
                text.contains("screen recording") ||
                text.contains("screen recorder")
    }

    private fun handleScreenRecordingNotification(sbn: StatusBarNotification) {
        systemRecordingKey = sbn.key
        val notification = sbn.notification ?: return

        // Extract actions: Stop, Pause, Resume
        val actions = notification.actions
        actions?.forEach { action ->
            val aTitle = (action.title?.toString() ?: "").lowercase()
            if (aTitle.contains("stop") || aTitle.contains("end") || aTitle.contains("finish")) {
                activeStopAction = action
            } else if (aTitle.contains("pause")) {
                activePauseAction = action
            } else if (aTitle.contains("resume")) {
                activeResumeAction = action
            }
        }
        if (activeStopAction == null && !actions.isNullOrEmpty() && actions.size == 1) {
            activeStopAction = actions[0]
        }

        // Connect functions so island buttons trigger real recording actions
        IslandStateManager.onStopRecording = {
            try {
                activeStopAction?.actionIntent?.send()
            } catch (e: Exception) {
                Log.e("NotificationReceiver", "Error sending stop intent", e)
            }
            recordingTimerJob?.cancel()
            recordingTimerJob = null
            systemRecordingKey = null
            activeStopAction = null
            activePauseAction = null
            activeResumeAction = null
            IslandStateManager.updateRecordingState(ScreenRecordingState(isRecording = false))
        }

        IslandStateManager.onTogglePauseRecording = {
            val currentState = IslandStateManager.screenRecordingState.value
            if (currentState.isPaused) {
                try {
                    activeResumeAction?.actionIntent?.send() ?: activePauseAction?.actionIntent?.send()
                } catch (e: Exception) {
                    Log.e("NotificationReceiver", "Error sending resume intent", e)
                }
                if (recordingPausedAtMs > 0) {
                    recordingStartTimeMs += (System.currentTimeMillis() - recordingPausedAtMs)
                    recordingPausedAtMs = 0L
                }
                IslandStateManager.updateRecordingState(currentState.copy(isPaused = false))
            } else {
                try {
                    activePauseAction?.actionIntent?.send()
                } catch (e: Exception) {
                    Log.e("NotificationReceiver", "Error sending pause intent", e)
                }
                recordingPausedAtMs = System.currentTimeMillis()
                IslandStateManager.updateRecordingState(currentState.copy(isPaused = true))
            }
        }

        if (recordingTimerJob == null || !IslandStateManager.screenRecordingState.value.isRecording) {
            val postTime = sbn.postTime
            recordingStartTimeMs = if (postTime > 0 && postTime <= System.currentTimeMillis()) postTime else System.currentTimeMillis()
            recordingPausedAtMs = 0L
            val initialSeconds = ((System.currentTimeMillis() - recordingStartTimeMs) / 1000).coerceAtLeast(0L)
            IslandStateManager.updateRecordingState(
                ScreenRecordingState(isRecording = true, durationSeconds = initialSeconds, isPaused = false)
            )
            recordingTimerJob = serviceScope.launch {
                while (isActive) {
                    delay(1000)
                    val currentState = IslandStateManager.screenRecordingState.value
                    if (!currentState.isPaused) {
                        val elapsed = ((System.currentTimeMillis() - recordingStartTimeMs) / 1000).coerceAtLeast(0L)
                        IslandStateManager.updateRecordingState(
                            currentState.copy(isRecording = true, durationSeconds = elapsed)
                        )
                    }
                }
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        if (!currentSettings.showNotifications && !currentSettings.showLiveProgress && !currentSettings.showScreenRecording) return

        val packageName = sbn.packageName
        if (packageName == this.packageName) return // Ignore self
        if (currentSettings.blockedPackages.contains(packageName)) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // 0. If a "screen recording saved" notification arrives, immediately shut down recording state
        if (isScreenRecordingSavedNotification(sbn, notification)) {
            val pkg = sbn.packageName.lowercase()
            val channelId = notification.channelId?.lowercase() ?: ""
            if (pkg.contains("screenrecord") || pkg == "com.android.systemui" || pkg == "com.google.android.systemui" || channelId.contains("screen_record") || systemRecordingKey == sbn.key) {
                systemRecordingKey = null
                recordingTimerJob?.cancel()
                recordingTimerJob = null
                activeStopAction = null
                activePauseAction = null
                activeResumeAction = null
                IslandStateManager.updateRecordingState(ScreenRecordingState(isRecording = false))
                return
            }
        }

        // 1. Detect system screen recording notifications
        if (isScreenRecordingNotification(sbn, notification)) {
            if (currentSettings.showScreenRecording) {
                handleScreenRecordingNotification(sbn)
            }
            return
        }

        // 2. Skip media notifications (Media controls are handled exclusively via MediaSessionManager with slider & controls)
        if (isMediaNotification(sbn, notification)) {
            return
        }

        // 3. Extract live progress tracking (e.g. Google Play Store, downloads, transfers)
        val progress = extras.getInt(Notification.EXTRA_PROGRESS, -1)
        val maxProgress = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val isIndeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false)
        val hasProgress = maxProgress > 0 || isIndeterminate || (progress >= 0 && maxProgress > 0)

        // Separate check: live progress notifications vs normal notification alerts
        if (hasProgress) {
            if (!currentSettings.showLiveProgress) return
        } else {
            if (!currentSettings.showNotifications) return
        }

        // Skip ongoing notifications UNLESS they report live progress (e.g. downloads)
        val isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
        if (isOngoing && !hasProgress) return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

        if (title.isNullOrBlank() && text.isNullOrBlank()) return

        val appName = try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }

        val iconBitmap = extractNotificationBitmap(notification, packageName)

        val actions = mutableListOf<NotificationAction>()
        notification.actions?.forEach { action ->
            if (action != null && !action.title.isNullOrBlank()) {
                val remoteInput = action.remoteInputs?.firstOrNull()
                val choices = remoteInput?.choices?.map { it.toString() } ?: emptyList()
                actions.add(
                    NotificationAction(
                        title = action.title.toString(),
                        actionIntent = action.actionIntent,
                        remoteInputKey = remoteInput?.resultKey,
                        remoteInputLabel = remoteInput?.label?.toString(),
                        remoteInputChoices = choices
                    )
                )
            }
        }

        val notifInfo = NotificationInfo(
            key = sbn.key,
            packageName = packageName,
            appName = appName,
            title = title ?: appName,
            text = text ?: "",
            iconBitmap = iconBitmap,
            contentIntent = notification.contentIntent,
            actions = actions.take(3),
            timestamp = sbn.postTime,
            progress = progress,
            maxProgress = maxProgress,
            isIndeterminate = isIndeterminate,
            isOngoing = isOngoing
        )

        IslandStateManager.updateNotification(notifInfo)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return
        if (systemRecordingKey == sbn.key || (sbn.notification != null && isScreenRecordingNotification(sbn, sbn.notification))) {
            systemRecordingKey = null
            recordingTimerJob?.cancel()
            recordingTimerJob = null
            activeStopAction = null
            activePauseAction = null
            activeResumeAction = null
            IslandStateManager.updateRecordingState(ScreenRecordingState(isRecording = false))
        }
        if (IslandStateManager.activeNotification.value?.key == sbn.key) {
            IslandStateManager.updateNotification(null)
        }
    }

    private fun extractNotificationBitmap(notification: Notification, packageName: String): Bitmap? {
        val largeIcon: Icon? = notification.getLargeIcon()
        if (largeIcon != null) {
            val drawable = largeIcon.loadDrawable(this)
            if (drawable != null) return drawableToBitmap(drawable)
        }
        val smallIcon: Icon? = notification.smallIcon
        if (smallIcon != null) {
            val drawable = smallIcon.loadDrawable(this)
            if (drawable != null) return drawableToBitmap(drawable)
        }
        return getAppIconBitmap(packageName)
    }

    private fun getAppIconBitmap(packageName: String): Bitmap? {
        return try {
            val drawable = packageManager.getApplicationIcon(packageName)
            drawableToBitmap(drawable)
        } catch (e: Exception) {
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun isMediaNotification(sbn: StatusBarNotification, notification: Notification): Boolean {
        // 1. Category is TRANSPORT
        if (notification.category == Notification.CATEGORY_TRANSPORT) return true

        // 2. Extras contains media session token or MediaStyle template
        val extras = notification.extras
        if (extras != null) {
            if (extras.containsKey(Notification.EXTRA_MEDIA_SESSION) ||
                extras.containsKey("android.mediaSession") ||
                extras.getString(Notification.EXTRA_TEMPLATE)?.contains("MediaStyle") == true
            ) {
                return true
            }
        }

        // 3. Sender is currently active media controller
        if (sbn.packageName == activeMediaController?.packageName) return true
        try {
            val componentName = ComponentName(this, NotificationReceiverService::class.java)
            val activeSessions = mediaSessionManager?.getActiveSessions(componentName)
            if (activeSessions?.any { it.packageName == sbn.packageName } == true) return true
        } catch (_: Exception) {}

        // 4. Playback actions present (e.g. Previous, Next, Play, Pause)
        notification.actions?.forEach { action ->
            val actionTitle = action?.title?.toString()?.lowercase(java.util.Locale.ROOT) ?: ""
            if (actionTitle.contains("pause") || actionTitle.contains("play") ||
                actionTitle.contains("skip") || actionTitle.contains("previous") ||
                actionTitle.contains("next") || actionTitle.contains("rewind") ||
                actionTitle.contains("fast forward")
            ) {
                return true
            }
        }

        return false
    }
}
