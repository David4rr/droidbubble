package com.droidbubble.app.island

import com.droidbubble.app.model.IslandPriority
import com.droidbubble.app.model.MediaInfo
import com.droidbubble.app.model.NotificationInfo
import com.droidbubble.app.model.ScreenRecordingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object IslandStateManager {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var popInJob: Job? = null

    private val _activeMedia = MutableStateFlow<MediaInfo?>(null)
    val activeMedia: StateFlow<MediaInfo?> = _activeMedia.asStateFlow()

    private val _activeNotification = MutableStateFlow<NotificationInfo?>(null)
    val activeNotification: StateFlow<NotificationInfo?> = _activeNotification.asStateFlow()

    private val _screenRecordingState = MutableStateFlow(ScreenRecordingState())
    val screenRecordingState: StateFlow<ScreenRecordingState> = _screenRecordingState.asStateFlow()

    private val _isExpanded = MutableStateFlow(false)
    val isExpanded: StateFlow<Boolean> = _isExpanded.asStateFlow()

    private val _expandedPriority = MutableStateFlow<IslandPriority?>(null)
    val expandedPriority: StateFlow<IslandPriority?> = _expandedPriority.asStateFlow()

    private val _isPopInActive = MutableStateFlow(false)
    val isPopInActive: StateFlow<Boolean> = _isPopInActive.asStateFlow()

    private var lastMediaPlayingTime = 0L

    // Callbacks for media transport controls
    var onPlayPause: (() -> Unit)? = null
    var onSkipNext: (() -> Unit)? = null
    var onSkipPrevious: (() -> Unit)? = null
    var onSeekTo: ((Long) -> Unit)? = null

    // Callbacks for screen recording controls
    var onStopRecording: (() -> Unit)? = null
    var onTogglePauseRecording: (() -> Unit)? = null

    // Callback to dismiss notification from listener
    var onDismissNotification: ((String) -> Unit)? = null

    val isRecordingActive: Boolean
        get() = _screenRecordingState.value.isRecording

    val isNotificationActive: Boolean
        get() = _activeNotification.value != null

    val isMediaActive: Boolean
        get() {
            val media = _activeMedia.value ?: return false
            // Keep active whenever a media session exists with a title so the island remains visible & expandable when paused!
            return media.title.isNotBlank()
        }

    val isProgressActive: Boolean
        get() = _activeNotification.value?.hasProgress == true

    val isOccupied: Boolean
        get() = isRecordingActive || isNotificationActive || isMediaActive

    // Live priority determination
    val currentPriority: IslandPriority
        get() {
            return when {
                _screenRecordingState.value.isRecording -> IslandPriority.SCREEN_RECORDING
                isProgressActive -> IslandPriority.PROGRESS
                _isPopInActive.value && _activeNotification.value != null -> IslandPriority.POP_NOTIFICATION
                _activeMedia.value?.isPlaying == true -> IslandPriority.MEDIA
                _activeNotification.value != null -> IslandPriority.POP_NOTIFICATION
                _activeMedia.value != null -> IslandPriority.MEDIA
                else -> IslandPriority.IDLE
            }
        }

    val activeExpandedPriority: IslandPriority
        get() = _expandedPriority.value ?: currentPriority

    fun updateMedia(media: MediaInfo?) {
        _activeMedia.value = media
        if (media?.isPlaying == true) {
            lastMediaPlayingTime = System.currentTimeMillis()
        }
    }

    fun updateNotification(notification: NotificationInfo?) {
        _activeNotification.value = notification
        if (notification != null) {
            if (notification.hasProgress) {
                // Live progress task: keep active continuously while downloading
                _isPopInActive.value = true
                popInJob?.cancel()
            } else {
                triggerPopIn()
            }
        }
    }

    fun triggerPopIn() {
        _isPopInActive.value = true
        popInJob?.cancel()
        popInJob = scope.launch {
            delay(4500) // Bubble pop-in for 4.5 seconds then auto-shrink to compact
            _isPopInActive.value = false
        }
    }

    fun dismissCurrentNotification(): Boolean {
        val notif = _activeNotification.value ?: return false
        // Live tasks with progress cannot be dismissed by swiping
        if (notif.hasProgress) return false
        onDismissNotification?.invoke(notif.key)
        _activeNotification.value = null
        _isPopInActive.value = false
        return true
    }

    fun updateRecordingState(state: ScreenRecordingState) {
        _screenRecordingState.value = state
    }

    fun expand(priority: IslandPriority? = null) {
        _expandedPriority.value = priority
        _isExpanded.value = true
    }

    fun collapse() {
        _isExpanded.value = false
        _expandedPriority.value = null
    }

    fun toggleExpand(priority: IslandPriority? = null) {
        if (_isExpanded.value) {
            collapse()
        } else {
            expand(priority)
        }
    }
}
