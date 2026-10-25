package com.droidbubble.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.droidbubble.app.model.IslandSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "island_settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val IS_ENABLED = booleanPreferencesKey("is_enabled")
        val WIDTH_DP = intPreferencesKey("width_dp")
        val HEIGHT_DP = intPreferencesKey("height_dp")
        val CORNER_RADIUS_DP = intPreferencesKey("corner_radius_dp")
        val TOP_OFFSET_DP = intPreferencesKey("top_offset_dp")
        val HORIZONTAL_OFFSET_DP = intPreferencesKey("horizontal_offset_dp")
        val SHOW_OUTLINE = booleanPreferencesKey("show_outline")
        val OUTLINE_COLOR_ARGB = longPreferencesKey("outline_color_argb")
        val OUTLINE_THICKNESS_DP = floatPreferencesKey("outline_thickness_dp")
        val EXPANDED_SCALE = floatPreferencesKey("expanded_scale")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val SHOW_MEDIA = booleanPreferencesKey("show_media")
        val SHOW_NOTIFICATIONS = booleanPreferencesKey("show_notifications")
        val SHOW_LIVE_PROGRESS = booleanPreferencesKey("show_live_progress")
        val SHOW_SCREEN_RECORDING = booleanPreferencesKey("show_screen_recording")
        val ANIMATION_SPEED = floatPreferencesKey("animation_speed")
        val BLOCKED_PACKAGES = stringSetPreferencesKey("blocked_packages")
        val RESTART_ON_BOOT = booleanPreferencesKey("restart_on_boot")
    }

    val settingsFlow: Flow<IslandSettings> = context.dataStore.data.map { preferences ->
        IslandSettings(
            isEnabled = preferences[PreferencesKeys.IS_ENABLED] ?: true,
            widthDp = preferences[PreferencesKeys.WIDTH_DP] ?: 180,
            heightDp = preferences[PreferencesKeys.HEIGHT_DP] ?: 36,
            cornerRadiusDp = preferences[PreferencesKeys.CORNER_RADIUS_DP] ?: 22,
            topOffsetDp = preferences[PreferencesKeys.TOP_OFFSET_DP] ?: 12,
            horizontalOffsetDp = preferences[PreferencesKeys.HORIZONTAL_OFFSET_DP] ?: 0,
            showOutline = preferences[PreferencesKeys.SHOW_OUTLINE] ?: true,
            outlineColorArgb = preferences[PreferencesKeys.OUTLINE_COLOR_ARGB] ?: 0x33FFFFFF,
            outlineThicknessDp = preferences[PreferencesKeys.OUTLINE_THICKNESS_DP] ?: 1.5f,
            expandedScale = preferences[PreferencesKeys.EXPANDED_SCALE] ?: 1.0f,
            hapticsEnabled = preferences[PreferencesKeys.HAPTICS_ENABLED] ?: true,
            showMedia = preferences[PreferencesKeys.SHOW_MEDIA] ?: true,
            showNotifications = preferences[PreferencesKeys.SHOW_NOTIFICATIONS] ?: false,
            showLiveProgress = preferences[PreferencesKeys.SHOW_LIVE_PROGRESS] ?: true,
            showScreenRecording = preferences[PreferencesKeys.SHOW_SCREEN_RECORDING] ?: true,
            animationSpeed = preferences[PreferencesKeys.ANIMATION_SPEED] ?: 1.0f,
            blockedPackages = preferences[PreferencesKeys.BLOCKED_PACKAGES] ?: emptySet(),
            restartOnBoot = preferences[PreferencesKeys.RESTART_ON_BOOT] ?: true
        )
    }

    suspend fun updateSettings(update: (IslandSettings) -> IslandSettings) {
        context.dataStore.edit { preferences ->
            val current = IslandSettings(
                isEnabled = preferences[PreferencesKeys.IS_ENABLED] ?: true,
                widthDp = preferences[PreferencesKeys.WIDTH_DP] ?: 180,
                heightDp = preferences[PreferencesKeys.HEIGHT_DP] ?: 36,
                cornerRadiusDp = preferences[PreferencesKeys.CORNER_RADIUS_DP] ?: 22,
                topOffsetDp = preferences[PreferencesKeys.TOP_OFFSET_DP] ?: 12,
                horizontalOffsetDp = preferences[PreferencesKeys.HORIZONTAL_OFFSET_DP] ?: 0,
                showOutline = preferences[PreferencesKeys.SHOW_OUTLINE] ?: true,
                outlineColorArgb = preferences[PreferencesKeys.OUTLINE_COLOR_ARGB] ?: 0x33FFFFFF,
                outlineThicknessDp = preferences[PreferencesKeys.OUTLINE_THICKNESS_DP] ?: 1.5f,
                expandedScale = preferences[PreferencesKeys.EXPANDED_SCALE] ?: 1.0f,
                hapticsEnabled = preferences[PreferencesKeys.HAPTICS_ENABLED] ?: true,
                showMedia = preferences[PreferencesKeys.SHOW_MEDIA] ?: true,
                showNotifications = preferences[PreferencesKeys.SHOW_NOTIFICATIONS] ?: false,
                showLiveProgress = preferences[PreferencesKeys.SHOW_LIVE_PROGRESS] ?: true,
                showScreenRecording = preferences[PreferencesKeys.SHOW_SCREEN_RECORDING] ?: true,
                animationSpeed = preferences[PreferencesKeys.ANIMATION_SPEED] ?: 1.0f,
                blockedPackages = preferences[PreferencesKeys.BLOCKED_PACKAGES] ?: emptySet(),
                restartOnBoot = preferences[PreferencesKeys.RESTART_ON_BOOT] ?: true
            )

            val newSettings = update(current)

            preferences[PreferencesKeys.IS_ENABLED] = newSettings.isEnabled
            preferences[PreferencesKeys.WIDTH_DP] = newSettings.widthDp
            preferences[PreferencesKeys.HEIGHT_DP] = newSettings.heightDp
            preferences[PreferencesKeys.CORNER_RADIUS_DP] = newSettings.cornerRadiusDp
            preferences[PreferencesKeys.TOP_OFFSET_DP] = newSettings.topOffsetDp
            preferences[PreferencesKeys.HORIZONTAL_OFFSET_DP] = newSettings.horizontalOffsetDp
            preferences[PreferencesKeys.SHOW_OUTLINE] = newSettings.showOutline
            preferences[PreferencesKeys.OUTLINE_COLOR_ARGB] = newSettings.outlineColorArgb
            preferences[PreferencesKeys.OUTLINE_THICKNESS_DP] = newSettings.outlineThicknessDp
            preferences[PreferencesKeys.EXPANDED_SCALE] = newSettings.expandedScale
            preferences[PreferencesKeys.HAPTICS_ENABLED] = newSettings.hapticsEnabled
            preferences[PreferencesKeys.SHOW_MEDIA] = newSettings.showMedia
            preferences[PreferencesKeys.SHOW_NOTIFICATIONS] = newSettings.showNotifications
            preferences[PreferencesKeys.SHOW_LIVE_PROGRESS] = newSettings.showLiveProgress
            preferences[PreferencesKeys.SHOW_SCREEN_RECORDING] = newSettings.showScreenRecording
            preferences[PreferencesKeys.ANIMATION_SPEED] = newSettings.animationSpeed
            preferences[PreferencesKeys.BLOCKED_PACKAGES] = newSettings.blockedPackages
            preferences[PreferencesKeys.RESTART_ON_BOOT] = newSettings.restartOnBoot
        }
    }
}
