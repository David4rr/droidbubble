package com.droidbubble.app.island

import android.app.PendingIntent
import android.graphics.Bitmap
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidbubble.app.model.IslandPriority
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.model.MediaInfo
import com.droidbubble.app.model.NotificationInfo
import com.droidbubble.app.model.ScreenRecordingState
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun IslandOverlayRoot(
    settings: IslandSettings,
    onBoundsChange: (widthPx: Int, heightPx: Int) -> Unit
) {
    val isExpanded by IslandStateManager.isExpanded.collectAsState()
    val isPopIn by IslandStateManager.isPopInActive.collectAsState()
    val media by IslandStateManager.activeMedia.collectAsState()
    val notification by IslandStateManager.activeNotification.collectAsState()
    val recording by IslandStateManager.screenRecordingState.collectAsState()

    val view = LocalView.current

    val currentMedia = media
    val currentNotif = notification
    val isRecording = recording.isRecording
    val isNotification = currentNotif != null
    val isMedia = currentMedia != null && (currentMedia.isPlaying || currentMedia.title.isNotBlank())

    val isOccupied = isRecording || isNotification || isMedia

    // Determine dual-island state (when 2+ activities occupy the island)
    val activeCount = (if (isRecording) 1 else 0) + (if (isNotification) 1 else 0) + (if (isMedia) 1 else 0)
    val isDual = activeCount >= 2

    // Primary & Secondary activity assignment
    val primaryPriority: IslandPriority = when {
        isMedia -> IslandPriority.MEDIA
        isRecording -> IslandPriority.SCREEN_RECORDING
        isNotification -> IslandPriority.POP_NOTIFICATION
        else -> IslandPriority.IDLE
    }

    val secondaryPriority: IslandPriority? = when {
        isDual && isMedia && isNotification -> IslandPriority.POP_NOTIFICATION
        isDual && isMedia && isRecording -> IslandPriority.SCREEN_RECORDING
        isDual && isRecording && isNotification -> IslandPriority.POP_NOTIFICATION
        else -> null
    }

    // Animation Specs:
    // Expand: subtle bounce outwards (spring damping 0.74f)
    // Minimise: smooth ease-in-and-out curve that minimises smoothly before reaching the camera cutout
    val currentAnimSpecDp = if (isExpanded) {
        spring<androidx.compose.ui.unit.Dp>(
            dampingRatio = 0.74f, // subtle organic overshoot bounce on expand
            stiffness = 380f
        )
    } else {
        tween<androidx.compose.ui.unit.Dp>(
            durationMillis = 340,
            easing = CubicBezierEasing(0.36f, 0.0f, 0.16f, 1.0f) // smooth ease-in-and-out curve
        )
    }

    val currentAnimSpecFloat = if (isExpanded) {
        spring<Float>(
            dampingRatio = 0.74f,
            stiffness = 380f
        )
    } else {
        tween<Float>(
            durationMillis = 340,
            easing = CubicBezierEasing(0.36f, 0.0f, 0.16f, 1.0f)
        )
    }

    val expandProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = currentAnimSpecFloat,
        label = "expandProgress"
    )

    // Target dimensions:
    // When minimised, height is STRICTLY fixed at settings.heightDp.dp (never changes or mixes up!)
    // Resizing is purely horizontal: width shrinks to 0 when idle, expands to widthDp with 1 action,
    // or splits into (widthDp * 0.74) + circular auxiliary bubble in dual mode.
    val targetMainWidth = when {
        isExpanded -> (340.dp * settings.expandedScale)
        !isOccupied -> 0.dp // Disappears into the middle horizontally!
        isDual -> (settings.widthDp * 0.74f).dp // Main pill centered around punch-hole in dual mode
        else -> settings.widthDp.dp
    }

    val targetHeight = when {
        isExpanded -> (185.dp * settings.expandedScale)
        else -> settings.heightDp.dp
    }

    val targetCornerRadius = when {
        isExpanded -> 28.dp
        else -> settings.cornerRadiusDp.dp
    }

    val showSecondaryCard = activeCount >= 2 && secondaryPriority != null
    val targetCard2Height = if (isExpanded && showSecondaryCard) (125.dp * settings.expandedScale) else 0.dp
    val targetCard2Alpha = if (isExpanded && showSecondaryCard) 1f else 0f

    val targetBubbleSize = when {
        isDual && !isExpanded -> settings.heightDp.dp
        else -> 0.dp
    }

    val targetGap = when {
        isDual && !isExpanded -> 8.dp
        else -> 0.dp
    }

    val targetTopOffset = if (isExpanded) {
        (settings.topOffsetDp + 46).dp // Sits way below initial position (comfortably below status bar) when expanded!
    } else {
        settings.topOffsetDp.dp // Docks right over the punch hole when minimised
    }

    val targetCardSpacing = if (isExpanded && showSecondaryCard) 10.dp else 0.dp

    val animatedMainWidth by animateDpAsState(targetValue = targetMainWidth, animationSpec = currentAnimSpecDp, label = "mainWidth")
    val animatedHeight by animateDpAsState(targetValue = targetHeight, animationSpec = currentAnimSpecDp, label = "height")
    val animatedCornerRadius by animateDpAsState(targetValue = targetCornerRadius, animationSpec = currentAnimSpecDp, label = "cornerRadius")
    val animatedCard2Height by animateDpAsState(targetValue = targetCard2Height, animationSpec = currentAnimSpecDp, label = "card2Height")
    val animatedCard2Alpha by animateFloatAsState(targetValue = targetCard2Alpha, animationSpec = currentAnimSpecFloat, label = "card2Alpha")
    val animatedBubbleSize by animateDpAsState(targetValue = targetBubbleSize, animationSpec = currentAnimSpecDp, label = "bubbleSize")
    val animatedGap by animateDpAsState(targetValue = targetGap, animationSpec = currentAnimSpecDp, label = "gap")
    val animatedTopOffset by animateDpAsState(targetValue = targetTopOffset, animationSpec = currentAnimSpecDp, label = "topOffset")
    val animatedCardSpacing by animateDpAsState(targetValue = targetCardSpacing, animationSpec = currentAnimSpecDp, label = "cardSpacing")

    // Clamped height: NEVER bounces less than base height when collapsing from expanded!
    val minPillHeight = settings.heightDp.dp
    val displayHeight = if (isExpanded) animatedHeight else animatedHeight.coerceAtLeast(minPillHeight)

    fun triggerHaptic() {
        if (settings.hapticsEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    // Outer container: full touch bounds when expanded to capture outside tap, or exact bounds when minimised
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        // Tap outside backdrop when expanded (active during whole expand and collapse animation)
        if (expandProgress > 0.05f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        triggerHaptic()
                        IslandStateManager.collapse()
                    }
            )
        }

        // Main Island Container: Positioned at animated top offset (smooth slide between compact punch hole and expanded position!)
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = settings.horizontalOffsetDp.dp.roundToPx(),
                        y = animatedTopOffset.roundToPx()
                    )
                },
            contentAlignment = Alignment.TopCenter
        ) {
            // Cards Stack (Card 1 + Card 2)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(animatedCardSpacing),
                modifier = if (isExpanded) {
                    Modifier.pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount < -15f) {
                                triggerHaptic()
                                IslandStateManager.collapse()
                            }
                        }
                    }
                } else Modifier
            ) {
                // Card 1: Main Pill morphing smoothly into Primary Expanded Card
                Box(
                    modifier = Modifier
                        .width(animatedMainWidth)
                        .height(displayHeight)
                        .clipToBounds()
                        .shadow(
                            elevation = if (isExpanded) 20.dp else (if (isOccupied) 8.dp else 0.dp),
                            shape = RoundedCornerShape(animatedCornerRadius)
                        )
                        .clip(RoundedCornerShape(animatedCornerRadius))
                        .background(if (animatedMainWidth > 2.dp) Color.Black else Color.Transparent)
                        .then(
                            if (settings.showOutline && animatedMainWidth > 8.dp) {
                                Modifier.border(
                                    width = settings.outlineThicknessDp.dp,
                                    color = Color(settings.outlineColorArgb),
                                    shape = RoundedCornerShape(animatedCornerRadius)
                                )
                            } else Modifier
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!isExpanded && isOccupied) {
                                triggerHaptic()
                                IslandStateManager.expand()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Smooth crossfade:
                    // On expand: expanded controls fade in after the card expands
                    // On minimise: expanded controls fade out early without squishing, and compact elements fade in as pill docks
                    val expandedAlpha = if (isExpanded) {
                        ((expandProgress - 0.30f) / 0.70f).coerceIn(0f, 1f)
                    } else {
                        ((expandProgress - 0.45f) / 0.55f).coerceIn(0f, 1f)
                    }

                    val compactAlpha = if (isExpanded) {
                        (1f - (expandProgress * 3.0f)).coerceIn(0f, 1f)
                    } else {
                        (1f - (expandProgress * 1.8f)).coerceIn(0f, 1f)
                    }

                    // 1. Compact Content: strictly clear 48dp center over punch-hole
                    if (compactAlpha > 0.01f && animatedMainWidth > 24.dp) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = compactAlpha },
                            contentAlignment = Alignment.Center
                        ) {
                            CompactContent(
                                priority = primaryPriority,
                                media = currentMedia,
                                notification = currentNotif,
                                recording = recording,
                                isCompactDual = isDual
                            )
                        }
                    }

                    // 2. Expanded Card 1 Content (e.g. Media Player or Primary Notification)
                    if (expandedAlpha > 0.01f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp)
                                .graphicsLayer {
                                    alpha = expandedAlpha
                                    scaleX = 0.94f + (0.06f * expandedAlpha)
                                    scaleY = 0.94f + (0.06f * expandedAlpha)
                                }
                        ) {
                            ExpandedCardContent(
                                priority = primaryPriority,
                                media = currentMedia,
                                notification = currentNotif,
                                recording = recording,
                                onCollapse = {
                                    triggerHaptic()
                                    IslandStateManager.collapse()
                                }
                            )
                        }
                    }
                }

                // Card 2: Secondary Action Card (smoothly expands and collapses in dual mode!)
                if (animatedCard2Height > 1.dp) {
                    Box(
                        modifier = Modifier
                            .width(animatedMainWidth)
                            .height(animatedCard2Height)
                            .clipToBounds()
                            .graphicsLayer {
                                alpha = animatedCard2Alpha
                                scaleX = 0.92f + (0.08f * animatedCard2Alpha.coerceIn(0f, 1f))
                                scaleY = 0.92f + (0.08f * animatedCard2Alpha.coerceIn(0f, 1f))
                            }
                            .shadow(
                                elevation = 16.dp,
                                shape = RoundedCornerShape(animatedCornerRadius)
                            )
                            .clip(RoundedCornerShape(animatedCornerRadius))
                            .background(Color.Black)
                            .then(
                                if (settings.showOutline) {
                                    Modifier.border(
                                        width = settings.outlineThicknessDp.dp,
                                        color = Color(settings.outlineColorArgb),
                                        shape = RoundedCornerShape(animatedCornerRadius)
                                    )
                                } else Modifier
                            )
                            .padding(14.dp)
                    ) {
                        if (animatedCard2Alpha > 0.15f && secondaryPriority != null) {
                            ExpandedCardContent(
                                priority = secondaryPriority,
                                media = currentMedia,
                                notification = currentNotif,
                                recording = recording,
                                onCollapse = {
                                    triggerHaptic()
                                    IslandStateManager.collapse()
                                }
                            )
                        }
                    }
                }
            }

            // Auxiliary Circular Island Bubble: Placed strictly to the RIGHT of the main pill
            // Never overlaps or comes near the center punch-hole!
            if (animatedBubbleSize > 3.dp && isDual) {
                val bubbleOffsetXPx = with(LocalDensity.current) {
                    ((animatedMainWidth / 2) + animatedGap + (animatedBubbleSize / 2)).roundToPx()
                }

                val bubbleAlpha = (1f - (expandProgress * 3f)).coerceIn(0f, 1f)

                Box(
                    modifier = Modifier
                        .offset { IntOffset(x = bubbleOffsetXPx, y = 0) }
                        .size(animatedBubbleSize)
                        .graphicsLayer { alpha = bubbleAlpha }
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape
                        )
                        .clip(CircleShape)
                        .background(Color.Black)
                        .then(
                            if (settings.showOutline && animatedBubbleSize > 8.dp) {
                                Modifier.border(
                                    width = settings.outlineThicknessDp.dp,
                                    color = Color(settings.outlineColorArgb),
                                    shape = CircleShape
                                )
                            } else Modifier
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            triggerHaptic()
                            IslandStateManager.expand()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (animatedBubbleSize >= (settings.heightDp * 0.45f).dp && bubbleAlpha > 0.2f) {
                        AuxiliaryBubbleContent(
                            priority = secondaryPriority,
                            media = currentMedia,
                            notification = currentNotif,
                            recording = recording
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AuxiliaryBubbleContent(
    priority: IslandPriority?,
    media: MediaInfo?,
    notification: NotificationInfo?,
    recording: ScreenRecordingState
) {
    when (priority) {
        IslandPriority.SCREEN_RECORDING -> {
            PulsingRecordingDot()
        }
        IslandPriority.POP_NOTIFICATION -> {
            if (notification?.iconBitmap != null) {
                Image(
                    bitmap = notification.iconBitmap.asImageBitmap(),
                    contentDescription = "Notification Icon",
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        IslandPriority.MEDIA -> {
            if (media?.albumArt != null) {
                Image(
                    bitmap = media.albumArt.asImageBitmap(),
                    contentDescription = "Album Art",
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                )
            } else {
                EqualizerVisualizer(isPlaying = media?.isPlaying == true)
            }
        }
        else -> Unit
    }
}

@Composable
fun CompactContent(
    priority: IslandPriority,
    media: MediaInfo?,
    notification: NotificationInfo?,
    recording: ScreenRecordingState,
    isCompactDual: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Leading content: Pinned strictly to the left of the camera punch hole
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            when (priority) {
                IslandPriority.SCREEN_RECORDING -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PulsingRecordingDot()
                        if (!isCompactDual) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "REC",
                                color = Color(0xFFE5E5EA),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                IslandPriority.POP_NOTIFICATION -> {
                    if (notification?.iconBitmap != null) {
                        Image(
                            bitmap = notification.iconBitmap.asImageBitmap(),
                            contentDescription = "Notification Icon",
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color(0xFFE5E5EA),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IslandPriority.MEDIA -> {
                    if (media?.albumArt != null) {
                        Image(
                            bitmap = media.albumArt.asImageBitmap(),
                            contentDescription = "Album Art",
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(5.dp))
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Radio,
                            contentDescription = null,
                            tint = Color(0xFFA1A1AA),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IslandPriority.IDLE -> Unit
            }
        }

        // Camera punch-hole deadzone: STRICTLY 48dp EMPTY SPACE IN THE MIDDLE!
        // No UI elements will EVER enter or overlap the camera cutout area
        Spacer(modifier = Modifier.width(48.dp))

        // Trailing content: Pinned strictly to the right of the camera punch hole
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd
        ) {
            when (priority) {
                IslandPriority.SCREEN_RECORDING -> {
                    val minutes = recording.durationSeconds / 60
                    val seconds = recording.durationSeconds % 60
                    Text(
                        text = String.format(Locale.US, "%02d:%02d", minutes, seconds),
                        color = Color(0xFFE5E5EA),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IslandPriority.POP_NOTIFICATION -> {
                    Text(
                        text = notification?.title ?: "Notification",
                        color = Color(0xFFE5E5EA),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IslandPriority.MEDIA -> {
                    EqualizerVisualizer(isPlaying = media?.isPlaying == true)
                }

                IslandPriority.IDLE -> Unit
            }
        }
    }
}

@Composable
fun ExpandedCardContent(
    priority: IslandPriority,
    media: MediaInfo?,
    notification: NotificationInfo?,
    recording: ScreenRecordingState,
    onCollapse: () -> Unit
) {
    when (priority) {
        IslandPriority.SCREEN_RECORDING -> {
            ExpandedRecordingView(recording, onCollapse)
        }
        IslandPriority.POP_NOTIFICATION -> {
            ExpandedNotificationView(notification, onCollapse)
        }
        IslandPriority.MEDIA -> {
            ExpandedMediaView(media, onCollapse)
        }
        IslandPriority.IDLE -> {
            ExpandedIdleView(onCollapse)
        }
    }
}

@Composable
fun ExpandedMediaView(media: MediaInfo?, onCollapse: () -> Unit) {
    var isSeeking by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableFloatStateOf(0f) }

    val currentPosition = if (isSeeking) scrubPosition.toLong() else (media?.positionMs ?: 0L)
    val duration = (media?.durationMs ?: 0L).coerceAtLeast(1L)

    Column(modifier = Modifier.fillMaxSize()) {
        // Header: Album art + Title & Artist
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (media?.albumArt != null) {
                Image(
                    bitmap = media.albumArt.asImageBitmap(),
                    contentDescription = "Album Art",
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF212121)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = null,
                        tint = Color(0xFFA1A1AA),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = media?.title ?: "No Media Playing",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = media?.artist ?: "Unknown Artist",
                    color = Color(0xFFA1A1AA),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            EqualizerVisualizer(isPlaying = media?.isPlaying == true)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Progress Bar & Duration
        if (duration > 0) {
            Slider(
                value = (currentPosition.toFloat() / duration).coerceIn(0f, 1f),
                onValueChange = { frac ->
                    isSeeking = true
                    scrubPosition = frac * duration
                },
                onValueChangeFinished = {
                    isSeeking = false
                    IslandStateManager.onSeekTo?.invoke(scrubPosition.toLong())
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color(0xFFE5E5EA),
                    inactiveTrackColor = Color(0xFF2C2C2E)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(currentPosition),
                    color = Color(0xFF8E8E93),
                    fontSize = 10.sp
                )
                Text(
                    text = formatDuration(duration),
                    color = Color(0xFF8E8E93),
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Playback Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { IslandStateManager.onSkipPrevious?.invoke() },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.FastRewind, contentDescription = "Previous", tint = Color(0xFFE5E5EA))
            }

            Spacer(modifier = Modifier.width(16.dp))

            IconButton(
                onClick = { IslandStateManager.onPlayPause?.invoke() },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                Icon(
                    imageVector = if (media?.isPlaying == true) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            IconButton(
                onClick = { IslandStateManager.onSkipNext?.invoke() },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.FastForward, contentDescription = "Next", tint = Color(0xFFE5E5EA))
            }
        }
    }
}

@Composable
fun ExpandedNotificationView(notification: NotificationInfo?, onCollapse: () -> Unit) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (notification?.iconBitmap != null) {
                Image(
                    bitmap = notification.iconBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint = Color(0xFFE5E5EA),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification?.title ?: "Notification",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = notification?.appName ?: "",
                    color = Color(0xFFA1A1AA),
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = {
                    IslandStateManager.dismissCurrentNotification()
                    onCollapse()
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFF8E8E93))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = notification?.text ?: "",
            color = Color(0xFFE5E5EA),
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    // Open application
                    runCatching {
                        notification?.contentIntent?.send()
                        onCollapse()
                    }
                }
        )

        Spacer(modifier = Modifier.weight(1f))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            notification?.actions?.forEach { action ->
                FilledTonalButton(
                    onClick = {
                        runCatching { action.actionIntent?.send() }
                        IslandStateManager.dismissCurrentNotification()
                        onCollapse()
                    },
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF2C2C2E),
                        contentColor = Color(0xFFF4F4F5)
                    )
                ) {
                    Text(text = action.title, fontSize = 11.sp, maxLines = 1)
                }
            }

            FilledTonalButton(
                onClick = {
                    runCatching { notification?.contentIntent?.send() }
                    onCollapse()
                },
                modifier = Modifier.height(32.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFFE5E5EA),
                    contentColor = Color.Black
                )
            ) {
                Text(text = "Open", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ExpandedRecordingView(recording: ScreenRecordingState, onCollapse: () -> Unit) {
    val minutes = recording.durationSeconds / 60
    val seconds = recording.durationSeconds % 60
    val timerText = String.format(Locale.US, "%02d:%02d", minutes, seconds)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PulsingRecordingDot()
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Screen Recording",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = timerText,
                color = Color(0xFFE5E5EA),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = if (recording.isPaused) "Recording paused" else "Capturing screen audio & display",
            color = Color(0xFFA1A1AA),
            fontSize = 12.sp
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    IslandStateManager.onTogglePauseRecording?.invoke()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2C2C2E),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(
                    imageVector = if (recording.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (recording.isPaused) "Resume" else "Pause", fontSize = 12.sp)
            }

            Button(
                onClick = {
                    IslandStateManager.onStopRecording?.invoke()
                    onCollapse()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2C2C2E),
                    contentColor = Color(0xFFE5E5EA)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = null,
                    tint = Color(0xFFE04040),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Stop", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE5E5EA))
            }
        }
    }
}

@Composable
fun ExpandedIdleView(onCollapse: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Droidbubble Island",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Ready for Media, Notifications & Recording",
            color = Color(0xFFA1A1AA),
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        FilledTonalButton(
            onClick = onCollapse,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = Color(0xFF2C2C2E),
                contentColor = Color.White
            )
        ) {
            Text("Collapse", fontSize = 12.sp)
        }
    }
}

@Composable
fun PulsingRecordingDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = Modifier
            .size(10.dp)
            .scale(1f + (alpha * 0.15f))
            .clip(CircleShape)
            .background(Color(0xFFE04040).copy(alpha = alpha))
    )
}

@Composable
fun EqualizerVisualizer(isPlaying: Boolean) {
    val transition = rememberInfiniteTransition(label = "eq_bars")

    val h1 by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h1"
    )
    val h2 by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h2"
    )
    val h3 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h3"
    )

    val currentH1 = if (isPlaying) h1 else 0.3f
    val currentH2 = if (isPlaying) h2 else 0.5f
    val currentH3 = if (isPlaying) h3 else 0.2f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.height(16.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height((16 * currentH1).dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(Color(0xFFE5E5EA))
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height((16 * currentH2).dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(Color(0xFFE5E5EA))
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height((16 * currentH3).dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(Color(0xFFE5E5EA))
        )
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
