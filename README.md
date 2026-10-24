# Droidbubble 🫧

**Droidbubble** is an iPhone-style Dynamic Island built natively for Android using **Kotlin**, **Jetpack Compose**, and **Material 3 Expressive**. It renders an interactive, floating overlay (`TYPE_APPLICATION_OVERLAY`) over all applications, positioned over the camera cutout area, while remaining completely non-intrusive to background touch events.

---

## Key Features

1. **True Dual-Island Architecture**:
   - **Single Activity**: A compact pill displaying primary activity indicators (e.g. Album Art + Animated Visualizer for media, or Pulsing Red Dot + Timer for screen recording, or App Icon + Title for notifications).
   - **Multi-Activity Split (Dual Island)**: When multiple activities are active simultaneously (e.g., Music playing + Notification received, or Music playing + Screen recording), the island splits into two elements: the primary pill and an auxiliary circular bubble that expands out of the main island.
   - **Zero-Width Idle Collapse**: When nothing is occupying the island, it smoothly collapses into the center (0 width) while maintaining its height, remaining completely invisible and non-intrusive. When an activity arrives, it smoothly springs outward with a natural bounce.

2. **Refined Spring Physics**:
   - Finely tuned spring animations (`dampingRatio = 0.82f`, `stiffness = 450f`) offering subtle, organic, overshoot motion without wild bouncing.
   - Mathematically clamped collapse motion preventing the pill height from ever bouncing below its minimum height when collapsing from the expanded card.

3. **Rich Media Controls**:
   - Intercepts playback from Spotify, YouTube, SoundCloud, YouTube Music, etc., via `MediaSessionManager`.
   - Dedicated interactive expanded card featuring high-resolution album art, track title, artist name, live equalizer, scrubbable progress bar with current/total timestamp, and media controls (Previous, Play/Pause, Next).
   - Audio transport notifications are filtered out so playback controls always open directly into the rich media player card.

4. **Interactive Notifications**:
   - Intercepts incoming notifications via `NotificationListenerService`.
   - Compact bubble pop-in with app icon and sender preview, auto-shrinking after a few seconds.
   - Expanded card displays app icon, title, message body, and direct quick action buttons (`PendingIntent`).
   - Supports per-app notification filtering.

5. **Screen Recording Indicator & Controls**:
   - Integrates with Android's `MediaProjection` foreground service.
   - Displays a pulsing red recording dot and live timer in compact state.
   - Expanded view provides stop and pause controls.

6. **Material 3 Expressive Settings UI (Single-Page)**:
   - Single scrollable settings page featuring collapsible `LargeTopAppBar`, grouped M3 cards, Live Preview, and custom dialog pickers.
   - Auto-hiding permissions section: individual permission items disappear once granted; the entire section disappears once all permissions are active.
   - Live DataStore persistence: all configuration adjustments update immediately on the floating overlay.

---

## Architecture & Package Structure

```
com.droidbubble.app
├── data
│   └── SettingsRepository.kt          # DataStore Preferences persistence
├── island
│   ├── IslandStateManager.kt          # Singleton state coordinator (media, notifs, recording, dual island)
│   └── IslandView.kt                  # Compose UI, spring animations, compact pills, expanded cards
├── model
│   ├── IslandEvent.kt                 # MediaInfo, NotificationInfo, ScreenRecordingState
│   └── IslandSettings.kt              # App configuration data class
├── service
│   ├── BootReceiver.kt                # Restores service after device reboot
│   ├── IslandOverlayService.kt        # TYPE_APPLICATION_OVERLAY foreground service with CustomLifecycleOwner
│   ├── NotificationReceiverService.kt # NotificationListenerService & MediaSessionManager controller
│   └── ScreenRecordingService.kt      # MediaProjection foreground service
└── ui
    └── main
        ├── MainScreen.kt              # M3 Expressive settings screen & Live Island preview
        ├── MainScreenViewModel.kt     # State holder & installed apps loader
        └── PermissionHelper.kt        # System permission verification & intent dispatchers
```

---

## Build & Installation

### Prerequisites
- Android SDK with platform `android-36` or `android-35`
- Java Development Kit (JDK) 17 or 21 (e.g. Android Studio JBR)
- Connected Android device with USB debugging enabled (API 29+)

### Build & Deploy Commands
```powershell
# Set Java 17/21
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"

# Build debug APK
.\gradlew.bat assembleDebug

# Install to connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch application
adb shell am start -n com.droidbubble.app/.MainActivity
```

---

## Manual Test Cases

### Test Case 1: Permissions Auto-Hiding
1. Launch Droidbubble on a fresh install.
2. Verify the **Required Setup** section appears with pending permissions (Overlay, Notification Listener, Notification Posting, Battery Optimization).
3. Tap each permission button and grant access in the system settings dialog.
4. Return to Droidbubble: observe that each granted item disappears immediately upon return.
5. Once all 4 permissions are granted, verify the entire **Required Setup** section completely disappears, leaving only the Island Customisation and Features sections.

### Test Case 2: Zero-Width Idle Collapse
1. Ensure no media is playing, no active notifications exist, and screen recording is inactive.
2. Look at the top center of the screen: verify that the island has completely disappeared horizontally (width = 0) and is not blocking status bar touches.
3. Start any media playback (e.g. YouTube or Spotify): observe the island smoothly spring outwards from the center to its pill width with a subtle, natural bounce.

### Test Case 3: Media Mode, Directional Springs & Punch-Hole Deadzone
1. Open YouTube or Spotify and play any song or video.
2. Verify the compact pill displays the album art / radio icon on the left, soft silver equalizer bars (`#E5E5EA`) on the right, and **strictly an empty 48dp deadzone in the exact center** directly over the physical camera punch-hole.
3. Tap the pill: observe it smoothly expand into the rich media player card with a subtle, organic bounce (`dampingRatio = 0.74f`).
4. Inside the expanding card:
   - Compact elements smoothly fade out as the card expands.
   - Expanded player controls (album art, title, artist, progress slider, timestamps, and playback buttons) smoothly fade in with an organic scale transition.
   - The color palette is consistently grayscale and low-saturation (silver active tracks, graphite inactive tracks, pure white play/pause pill).
5. Tap outside the card or swipe up:
   - Observe the card smoothly collapse back to the compact pill **critically damped with zero bounce** (`dampingRatio = 1.0f`).
   - The height never drops or bounces lower than the base pill height (`settings.heightDp`).

### Test Case 4: Dual-Island Multi-Activity & Stacked Card Expansion
1. Start media playback so the main island is active.
2. While media is playing, trigger a notification (or start screen recording from the app):
   ```powershell
   adb shell am broadcast -a com.droidbubble.app.ACTION_SIMULATE_NOTIFICATION -p com.droidbubble.app --es title WhatsApp --es text DinnerTonight
   ```
3. Observe the island immediately split into **two distinct islands**:
   - **Main Pill (center)**: Remains centered at `x = 0` with the 48dp empty deadzone in the middle (radio icon on left, equalizer bars on right).
   - **Auxiliary Circular Island (right)**: Smoothly pops out strictly to the right of the main pill, displaying the notification bell icon. Neither island obstructs the camera punch-hole.
4. Tap the island to expand:
   - Observe **both actions smoothly appear stacked vertically**:
     - **Top Card (185dp)**: Full Media Player with scrubber slider and transport buttons.
     - **Bottom Card (125dp)**: Notification card with matching rounded corners, dark theme, message preview, and action buttons.
5. Tap the outside backdrop or swipe up on the cards:
   - Observe both cards smoothly collapse back down into the compact dual island without any popping or stuttering.
   - Minimising works seamlessly and ordinarily.

### Test Case 5: Screen Recording Indicator
1. In the Droidbubble settings, tap **Start Test Screen Recording**.
2. Grant the MediaProjection capture prompt.
3. Observe the compact island show a pulsing muted-crimson recording dot (`#E04040`) and a live timer (`00:01`, `00:02`...).
4. Tap the island: verify the expanded view shows the stop and pause controls in cohesive dark charcoal and muted crimson styling.
5. Tap Stop: verify the recording saves to `Movies/Droidbubble` and the indicator clears.