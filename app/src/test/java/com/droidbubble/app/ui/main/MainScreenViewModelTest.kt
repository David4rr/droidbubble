package com.droidbubble.app.ui.main

import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.model.MediaInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MainScreenViewModelTest {
    @Test
    fun islandSettings_bubbleEffectEnabledByDefault() {
        val settings = IslandSettings()
        assertTrue(settings.bubbleEffectEnabled)
    }

    @Test
    fun islandSettings_newTogglesDisabledByDefault() {
        val settings = IslandSettings()
        org.junit.Assert.assertFalse(settings.dismissMediaOnPause)
        org.junit.Assert.assertFalse(settings.alwaysShowPill)
    }

    @Test
    fun mediaInfo_computeCurrentPosition_pausedReturnsBasePosition() {
        val media = MediaInfo(
            packageName = "com.google.android.youtube",
            title = "Test Song",
            artist = "Test Artist",
            isPlaying = false,
            durationMs = 180000L,
            positionMs = 45000L,
            lastPositionUpdateTimeMs = 100000L
        )
        assertEquals(45000L, media.computeCurrentPosition(nowMs = 120000L))
    }

    @Test
    fun mediaInfo_computeCurrentPosition_clampsToDuration() {
        val media = MediaInfo(
            packageName = "com.google.android.youtube",
            title = "Test Song",
            artist = "Test Artist",
            isPlaying = true,
            durationMs = 60000L,
            positionMs = 50000L,
            playbackSpeed = 1f,
            lastPositionUpdateTimeMs = 100000L
        )
        // Elapsed is 20s, 50s + 20s = 70s, but clamped to duration 60s
        assertEquals(60000L, media.computeCurrentPosition(nowMs = 120000L))
    }
}
