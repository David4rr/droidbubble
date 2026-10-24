package com.droidbubble.app.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.droidbubble.app.data.SettingsRepository
import com.droidbubble.app.island.IslandStateManager
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.model.MediaInfo
import com.droidbubble.app.model.NotificationAction
import com.droidbubble.app.model.NotificationInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotificationReceiverService : NotificationListenerService() {

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
            activeMediaController = null
            IslandStateManager.updateMedia(null)
        }
    }

    private val sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        updateActiveMediaController(controllers)
    }

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        serviceScope.launch {
            settingsRepository.settingsFlow.collectLatest {
                currentSettings = it
            }
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
        serviceScope.cancel()
    }

    private fun updateActiveMediaController(controllers: List<MediaController>?) {
        activeMediaController?.unregisterCallback(mediaCallback)
        activeMediaController = controllers?.firstOrNull()
        activeMediaController?.registerCallback(mediaCallback)
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

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: "Playing Audio"
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_AUTHOR)
            ?: controller.packageName

        var artBitmap = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)

        if (artBitmap == null) {
            // Fallback to app icon
            artBitmap = getAppIconBitmap(controller.packageName)
        }

        val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val position = playbackState?.position ?: 0L

        IslandStateManager.updateMedia(
            MediaInfo(
                packageName = controller.packageName,
                title = title,
                artist = artist,
                albumArt = artBitmap,
                isPlaying = isPlaying,
                durationMs = duration,
                positionMs = position
            )
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null || !currentSettings.showNotifications) return

        val packageName = sbn.packageName
        if (packageName == this.packageName) return // Ignore self
        if (currentSettings.blockedPackages.contains(packageName)) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // Skip media notifications (Media controls are handled exclusively via MediaSessionManager with slider & controls)
        if (isMediaNotification(sbn, notification)) {
            return
        }

        // Skip ongoing or progress-only system notifications unless active alert
        val isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
        if (isOngoing) return

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
                actions.add(NotificationAction(action.title.toString(), action.actionIntent))
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
            timestamp = sbn.postTime
        )

        IslandStateManager.updateNotification(notifInfo)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return
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
