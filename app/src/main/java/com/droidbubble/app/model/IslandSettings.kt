package com.droidbubble.app.model

data class IslandSettings(
    val isEnabled: Boolean = true,
    val widthDp: Int = 180,
    val heightDp: Int = 36,
    val cornerRadiusDp: Int = 22,
    val topOffsetDp: Int = 12,
    val horizontalOffsetDp: Int = 0,
    val showOutline: Boolean = true,
    val outlineColorArgb: Long = 0x33FFFFFF, // Sleek translucent silver
    val outlineThicknessDp: Float = 1.5f,
    val expandedScale: Float = 1.0f,
    val hapticsEnabled: Boolean = true,
    val showMedia: Boolean = true,
    val showNotifications: Boolean = true,
    val showScreenRecording: Boolean = true,
    val blockedPackages: Set<String> = emptySet(),
    val restartOnBoot: Boolean = true
)
