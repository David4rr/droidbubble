package com.droidbubble.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import kotlin.math.roundToInt
import android.provider.Settings
import com.droidbubble.app.model.MediaInfo
import com.droidbubble.app.model.NotificationInfo
import com.droidbubble.app.model.ScreenRecordingState
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.droidbubble.app.MainActivity
import com.droidbubble.app.R
import com.droidbubble.app.data.SettingsRepository
import com.droidbubble.app.island.IslandOverlayRoot
import com.droidbubble.app.island.IslandStateManager
import com.droidbubble.app.model.IslandSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class IslandOverlayService : Service() {

    companion object {
        const val CHANNEL_ID = "droidbubble_overlay_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.droidbubble.app.START_OVERLAY"
        const val ACTION_STOP = "com.droidbubble.app.STOP_OVERLAY"

        private var serviceInstance: IslandOverlayService? = null

        fun onAccessibilityServiceChanged(accessibilityService: IslandAccessibilityService?) {
            serviceInstance?.handleAccessibilityServiceChanged(accessibilityService)
        }

        fun start(context: Context) {
            val intent = Intent(context, IslandOverlayService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, IslandOverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var settingsRepository: SettingsRepository
    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private val lifecycleOwner = CustomLifecycleOwner()
    private var currentSettings = IslandSettings()

    override fun onBind(intent: Intent?): IBinder? = null

    private val debugReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.droidbubble.app.ACTION_SIMULATE_MEDIA" -> {
                    val title = intent.getStringExtra("title") ?: "Starboy"
                    val artist = intent.getStringExtra("artist") ?: "The Weeknd"
                    IslandStateManager.updateMedia(
                        MediaInfo(
                            packageName = "com.spotify.music",
                            title = title,
                            artist = artist,
                            isPlaying = true,
                            durationMs = 230000L,
                            positionMs = 65000L
                        )
                    )
                }
                "com.droidbubble.app.ACTION_SIMULATE_NOTIFICATION" -> {
                    val title = intent.getStringExtra("title") ?: "Sarah Jenkins"
                    val appName = intent.getStringExtra("appName") ?: "Messages"
                    val text = intent.getStringExtra("text") ?: "Dinner is ready! Can you pick up some dessert on your way home?"
                    val action1 = intent.getStringExtra("action1") ?: "Reply"
                    val action2 = intent.getStringExtra("action2")
                    val actionsList = mutableListOf<com.droidbubble.app.model.NotificationAction>()
                    if (action1.isNotBlank() && action1 != "none") {
                        actionsList.add(com.droidbubble.app.model.NotificationAction(action1, null))
                    }
                    if (!action2.isNullOrBlank()) {
                        actionsList.add(com.droidbubble.app.model.NotificationAction(action2, null))
                    }
                    IslandStateManager.updateNotification(
                        NotificationInfo(
                            key = "debug_notif_1",
                            packageName = "com.google.android.apps.messaging",
                            appName = appName,
                            title = title,
                            text = text,
                            actions = actionsList
                        )
                    )
                }
                "com.droidbubble.app.ACTION_SIMULATE_PROGRESS" -> {
                    val progress = intent.getIntExtra("progress", 68)
                    val maxProgress = intent.getIntExtra("max", 100)
                    val appName = intent.getStringExtra("appName") ?: "Google Play Store"
                    val title = intent.getStringExtra("title") ?: "Updating Spotify"
                    val text = intent.getStringExtra("text") ?: "45 MB / 66 MB • 3.2 MB/s"
                    IslandStateManager.updateNotification(
                        NotificationInfo(
                            key = "debug_progress_1",
                            packageName = "com.android.vending",
                            appName = appName,
                            title = title,
                            text = text,
                            progress = progress,
                            maxProgress = maxProgress,
                            isOngoing = true
                        )
                    )
                }
                "com.droidbubble.app.ACTION_SIMULATE_RECORDING" -> {
                    val isRec = intent.getBooleanExtra("isRecording", true)
                    IslandStateManager.updateRecordingState(
                        ScreenRecordingState(isRecording = isRec, durationSeconds = 42)
                    )
                }
                "com.droidbubble.app.ACTION_EXPAND" -> {
                    IslandStateManager.expand()
                }
                "com.droidbubble.app.ACTION_COLLAPSE" -> {
                    IslandStateManager.collapse()
                }
                "com.droidbubble.app.ACTION_RESET" -> {
                    IslandStateManager.updateMedia(null)
                    IslandStateManager.updateNotification(null)
                    IslandStateManager.updateRecordingState(ScreenRecordingState(isRecording = false))
                    IslandStateManager.collapse()
                }
                "com.droidbubble.app.ACTION_SIMULATE_CHARGING" -> {
                    val pct = intent.getIntExtra("percentage", 85)
                    val wattage = if (intent.hasExtra("wattage")) {
                        intent.getDoubleExtra("wattage", 33.0)
                    } else {
                        BatteryUtils.getChargingWattage(this@IslandOverlayService) ?: 33.0
                    }
                    IslandStateManager.triggerChargingEvent(pct, wattage)
                }
            }
        }
    }

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            if (action == Intent.ACTION_POWER_CONNECTED || action == "android.os.action.CHARGING") {
                val ctx = context ?: return
                val batteryIntent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                val pct = if (level >= 0 && scale > 0) {
                    ((level.toFloat() / scale.toFloat()) * 100).roundToInt().coerceIn(0, 100)
                } else {
                    100
                }
                val wattage = BatteryUtils.getChargingWattage(ctx, batteryIntent)
                IslandStateManager.triggerChargingEvent(pct, wattage)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        serviceInstance = this
        settingsRepository = SettingsRepository(this)
        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        createNotificationChannel()

        val filter = IntentFilter().apply {
            addAction("com.droidbubble.app.ACTION_SIMULATE_CHARGING")
            addAction("com.droidbubble.app.ACTION_SIMULATE_MEDIA")
            addAction("com.droidbubble.app.ACTION_SIMULATE_NOTIFICATION")
            addAction("com.droidbubble.app.ACTION_SIMULATE_PROGRESS")
            addAction("com.droidbubble.app.ACTION_SIMULATE_RECORDING")
            addAction("com.droidbubble.app.ACTION_EXPAND")
            addAction("com.droidbubble.app.ACTION_COLLAPSE")
            addAction("com.droidbubble.app.ACTION_RESET")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(debugReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(debugReceiver, filter)
        }

        val powerFilter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction("android.os.action.CHARGING")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(powerReceiver, powerFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(powerReceiver, powerFilter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            removeOverlay()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundService()

        if (Settings.canDrawOverlays(this)) {
            initOverlay()
        } else {
            stopSelf()
        }

        return START_STICKY
    }

    private fun startForegroundService() {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Droidbubble is active")
            .setContentText("Dynamic Island is displayed on your screen")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private var touchManager: IslandTouchManager? = null

    private fun disableMoveAnimation(params: WindowManager.LayoutParams) {
        try {
            val field = WindowManager.LayoutParams::class.java.getField("privateFlags")
            val current = field.getInt(params)
            field.setInt(params, current or 0x00000040)
        } catch (_: Exception) {}
    }



    fun handleAccessibilityServiceChanged(accessibilityService: IslandAccessibilityService?) {
        if (composeView != null) {
            removeOverlay()
            if (Settings.canDrawOverlays(this) && currentSettings.isEnabled) {
                initOverlay()
            }
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    private fun initOverlay() {
        if (composeView != null) return

        val aService = IslandAccessibilityService.instance
        windowManager = if (aService != null) {
            aService.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        } else {
            getSystemService(Context.WINDOW_SERVICE) as WindowManager
        }

        val density = resources.displayMetrics.density
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        val isOccupied = IslandStateManager.isOccupied
        val isCharging = IslandStateManager.isChargingActive
        val activeCount = (if (IslandStateManager.isRecordingActive) 1 else 0) +
                (if (IslandStateManager.isNotificationActive) 1 else 0) +
                (if (IslandStateManager.isMediaActive) 1 else 0) +
                (if (IslandStateManager.isProgressActive) 1 else 0)
        val isDual = !isCharging && activeCount >= 2

        val totalWidthDp = if (isCharging) {
            val chargingPillWidth = (currentSettings.widthDp + 20f).coerceAtLeast(140f)
            chargingPillWidth + 44f
        } else if (isDual) {
            val mainHalf = (currentSettings.widthDp * 0.74f) / 2f
            val rightEdge = mainHalf + 8f + currentSettings.heightDp + 22f
            (rightEdge * 2f).coerceAtLeast(currentSettings.widthDp + 44f)
        } else {
            currentSettings.widthDp + 44f
        }

        val initialWidth = if (isOccupied) {
            (totalWidthDp * density).toInt()
        } else 1
        val initialHeight = if (isOccupied) ((currentSettings.topOffsetDp + currentSettings.heightDp + 36) * density).toInt() else 1
        val initialFlags = if (isOccupied) flags else (flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

        val windowType = if (aService != null) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            initialWidth,
            initialHeight,
            windowType,
            initialFlags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = if (isOccupied && !isCharging) (currentSettings.horizontalOffsetDp * density).toInt() else 0
            y = 0
            windowAnimations = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        disableMoveAnimation(layoutParams!!)

        val view = ComposeView(aService ?: this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            fitsSystemWindows = false

            setContent {
                val liveSettings by settingsRepository.settingsFlow.collectAsState(initial = currentSettings)

                MaterialExpressiveTheme(
                    colorScheme = darkColorScheme(),
                    motionScheme = MotionScheme.expressive()
                ) {
                    IslandOverlayRoot(
                        settings = liveSettings,
                        onBoundsChange = { _, _ -> }
                    )
                }
            }
        }

        composeView = view

        try {
            windowManager?.addView(view, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Listen for expansion and state changes to resize WindowManager touch bounds
        serviceScope.launch {
            kotlinx.coroutines.flow.combine(
                IslandStateManager.isExpanded,
                IslandStateManager.activeMedia,
                IslandStateManager.activeNotification,
                IslandStateManager.screenRecordingState,
                IslandStateManager.activeCharging
            ) { expanded, media, notif, rec, charging ->
                Triple(
                    expanded,
                    charging != null,
                    (rec.isRecording || notif != null || (media != null && media.title.isNotBlank()))
                )
            }.collectLatest { (expanded, _, _) ->
                updateWindowState(expanded)
            }
        }

        // Listen for keyboard focus state to enable soft keyboard for inline replies
        serviceScope.launch {
            IslandStateManager.isKeyboardFocused.collectLatest { focused ->
                val view = composeView ?: return@collectLatest
                val params = layoutParams ?: return@collectLatest
                if (focused) {
                    params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
                } else {
                    params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                }
                disableMoveAnimation(params)
                runCatching { windowManager?.updateViewLayout(view, params) }
            }
        }

        // Listen for settings changes
        serviceScope.launch {
            settingsRepository.settingsFlow.collectLatest { settings ->
                currentSettings = settings
                if (!settings.isEnabled) {
                    removeOverlay()
                } else if (composeView == null) {
                    initOverlay()
                } else {
                    updateWindowState(IslandStateManager.isExpanded.value)
                }
            }
        }
    }

    private var isCollapsing = false
    private var collapseJob: kotlinx.coroutines.Job? = null
    private var wasExpanded = false

    private fun updateWindowState(expanded: Boolean) {
        val view = composeView ?: return
        val params = layoutParams ?: return
        val density = resources.displayMetrics.density

        if (expanded) {
            collapseJob?.cancel()
            isCollapsing = false
            wasExpanded = true
            // Full screen touch area to allow tapping outside to dismiss
            params.width = WindowManager.LayoutParams.MATCH_PARENT
            params.height = WindowManager.LayoutParams.MATCH_PARENT
            params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            params.x = 0
            params.y = 0
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
            disableMoveAnimation(params)
            runCatching { windowManager?.updateViewLayout(view, params) }
        } else {
            val isCharging = IslandStateManager.isChargingActive
            val isOccupied = IslandStateManager.isOccupied
            val activeCount = (if (IslandStateManager.isRecordingActive) 1 else 0) +
                    (if (IslandStateManager.isNotificationActive) 1 else 0) +
                    (if (IslandStateManager.isMediaActive) 1 else 0) +
                    (if (IslandStateManager.isProgressActive) 1 else 0)
            val isDual = !isCharging && activeCount >= 2

            val totalWidthDp = if (isCharging) {
                val chargingPillWidth = (currentSettings.widthDp + 20f).coerceAtLeast(140f)
                chargingPillWidth + 44f
            } else if (isDual) {
                val mainHalf = (currentSettings.widthDp * 0.74f) / 2f
                val rightEdge = mainHalf + 8f + currentSettings.heightDp + 22f
                (rightEdge * 2f).coerceAtLeast(currentSettings.widthDp + 44f)
            } else {
                currentSettings.widthDp + 44f
            }

            fun applyCompactBounds() {
                collapseJob?.cancel()
                if (isOccupied) {
                    params.width = (totalWidthDp * density).toInt()
                    params.x = (currentSettings.horizontalOffsetDp * density).toInt()
                    params.height = ((currentSettings.topOffsetDp + currentSettings.heightDp + 36) * density).toInt()
                    params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    params.y = 0
                    params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                    disableMoveAnimation(params)
                    runCatching { windowManager?.updateViewLayout(view, params) }
                } else {
                    params.width = 1
                    params.height = 1
                    params.x = 0
                    params.y = 0
                    params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    disableMoveAnimation(params)
                    runCatching { windowManager?.updateViewLayout(view, params) }
                }
            }

            if (isCharging) {
                isCollapsing = false
                collapseJob?.cancel()
            } else if (isCollapsing) {
                return
            }

            if (wasExpanded) {
                wasExpanded = false
                isCollapsing = true
                // Keep MATCH_PARENT for 400ms so the user sees the collapse animation smoothly finish in full!
                collapseJob = serviceScope.launch {
                    kotlinx.coroutines.delay(400)
                    isCollapsing = false
                    if (!IslandStateManager.isExpanded.value) {
                        applyCompactBounds()
                    }
                }
            } else if (!isOccupied) {
                // When unoccupied, keep bounds briefly for the smooth shrink-to-middle animation, then collapse
                collapseJob?.cancel()
                collapseJob = serviceScope.launch {
                    kotlinx.coroutines.delay(360)
                    if (!IslandStateManager.isOccupied && !IslandStateManager.isExpanded.value) {
                        params.width = 1
                        params.height = 1
                        params.x = 0
                        params.y = 0
                        params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        disableMoveAnimation(params)
                        runCatching { windowManager?.updateViewLayout(view, params) }
                    }
                }
            } else {
                applyCompactBounds()
            }
        }
    }

    private fun removeOverlay() {
        composeView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        composeView = null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceInstance === this) {
            serviceInstance = null
        }
        runCatching { unregisterReceiver(debugReceiver) }
        runCatching { unregisterReceiver(powerReceiver) }
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        removeOverlay()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Droidbubble Service",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Keeps Droidbubble Dynamic Island running smoothly"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}

// Custom LifecycleOwner for Compose in WindowManager Service
class CustomLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }

    fun performRestore(savedState: Bundle?) {
        savedStateRegistryController.performRestore(savedState)
    }
}
