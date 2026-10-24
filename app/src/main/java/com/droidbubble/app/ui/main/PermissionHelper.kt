package com.droidbubble.app.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

data class PermissionItem(
    val id: String,
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val createIntent: (Context) -> Intent
)

object PermissionHelper {

    fun isOverlayGranted(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun isNotificationListenerGranted(context: Context): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }

    fun isNotificationPostingGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    fun getPendingPermissions(context: Context): List<PermissionItem> {
        val list = mutableListOf<PermissionItem>()

        if (!isOverlayGranted(context)) {
            list.add(
                PermissionItem(
                    id = "overlay",
                    title = "Display over other apps",
                    description = "Required to render the Dynamic Island over all applications",
                    isGranted = false,
                    createIntent = { ctx ->
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${ctx.packageName}")
                        )
                    }
                )
            )
        }

        if (!isNotificationListenerGranted(context)) {
            list.add(
                PermissionItem(
                    id = "notification_listener",
                    title = "Notification & Media Access",
                    description = "Required to display active media playback and incoming notification alerts",
                    isGranted = false,
                    createIntent = {
                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    }
                )
            )
        }

        if (!isNotificationPostingGranted(context)) {
            list.add(
                PermissionItem(
                    id = "post_notifications",
                    title = "Post Notifications",
                    description = "Required for foreground service indicators on Android 13+",
                    isGranted = false,
                    createIntent = { ctx ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                            }
                        } else {
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:${ctx.packageName}")
                            )
                        }
                    }
                )
            )
        }

        if (!isBatteryOptimizationIgnored(context)) {
            list.add(
                PermissionItem(
                    id = "battery_optimization",
                    title = "Battery Unrestricted",
                    description = "Prevents Android from killing the Island service during background standby",
                    isGranted = false,
                    createIntent = { ctx ->
                        Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:${ctx.packageName}")
                        )
                    }
                )
            )
        }

        return list
    }
}
