package com.droidbubble.app.island

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidbubble.app.model.IslandPriority
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.model.MediaInfo
import com.droidbubble.app.model.NotificationInfo
import com.droidbubble.app.model.ScreenRecordingState
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

fun extractMediaAccentColor(bitmap: Bitmap?): Color {
    if (bitmap == null) return Color.Transparent
    return try {
        var redSum = 0L
        var greenSum = 0L
        var blueSum = 0L
        var count = 0L

        val stepX = (bitmap.width / 24).coerceAtLeast(1)
        val stepY = (bitmap.height / 24).coerceAtLeast(1)

        for (x in 0 until bitmap.width step stepX) {
            for (y in 0 until bitmap.height step stepY) {
                val pixel = bitmap.getPixel(x, y)
                val a = (pixel shr 24) and 0xff
                if (a > 60) {
                    val r = (pixel shr 16) and 0xff
                    val g = (pixel shr 8) and 0xff
                    val b = pixel and 0xff
                    redSum += r
                    greenSum += g
                    blueSum += b
                    count++
                }
            }
        }

        if (count > 0L) {
            val avgR = (redSum / count).toInt().coerceIn(0, 255)
            val avgG = (greenSum / count).toInt().coerceIn(0, 255)
            val avgB = (blueSum / count).toInt().coerceIn(0, 255)
            Color(avgR, avgG, avgB)
        } else {
            Color.Transparent
        }
    } catch (_: Exception) {
        Color.Transparent
    }
}

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
    val charging by IslandStateManager.activeCharging.collectAsState()

    val view = LocalView.current

    val currentMedia = media
    val currentNotif = notification
    val mediaArt = currentMedia?.albumArt ?: currentMedia?.appIcon
    val mediaAccentColor = remember(mediaArt) { extractMediaAccentColor(mediaArt) }
    val isRecording = recording.isRecording && settings.showScreenRecording
    val isProgress = currentNotif?.hasProgress == true && settings.showLiveProgress
    val isNotification = currentNotif != null && !isProgress && settings.showNotifications
    val isMedia = currentMedia != null && (currentMedia.isPlaying || currentMedia.title.isNotBlank()) && settings.showMedia
    val isCharging = charging != null

    val isOccupied = isRecording || isNotification || isMedia || isProgress || isCharging

    // Determine dual-island state (when 2+ activities occupy the island, temporarily overshadowed by charging)
    val activeCount = (if (isRecording) 1 else 0) +
            (if (isNotification) 1 else 0) +
            (if (isMedia) 1 else 0) +
            (if (isProgress) 1 else 0)
    val isDual = !isCharging && activeCount >= 2

    // Primary & Secondary activity assignment
    val primaryPriority: IslandPriority = when {
        isMedia -> IslandPriority.MEDIA
        isProgress -> IslandPriority.PROGRESS
        isRecording -> IslandPriority.SCREEN_RECORDING
        isNotification -> IslandPriority.POP_NOTIFICATION
        else -> IslandPriority.IDLE
    }

    val secondaryPriority: IslandPriority? = when {
        isDual && isMedia && isProgress -> IslandPriority.PROGRESS
        isDual && isMedia && isNotification -> IslandPriority.POP_NOTIFICATION
        isDual && isMedia && isRecording -> IslandPriority.SCREEN_RECORDING
        isDual && isProgress && isNotification -> IslandPriority.POP_NOTIFICATION
        isDual && isProgress && isRecording -> IslandPriority.SCREEN_RECORDING
        isDual && isRecording && isNotification -> IslandPriority.POP_NOTIFICATION
        else -> null
    }

    // Dynamic non-customizable outline colors (around 15% opacity):
    val redRecordingOutline = Color(0xFFFF3B30).copy(alpha = 0.50f)
    val defaultTaskOutline = Color.White.copy(alpha = 0.15f)
    val mediaOutline = if (mediaAccentColor != Color.Transparent) {
        mediaAccentColor.copy(alpha = 0.15f)
    } else {
        defaultTaskOutline
    }

    // Card 1 (Main Island):
    val primaryOutlineColor = when {
        isCharging -> Color(0xFF30D158).copy(alpha = 0.30f)
        primaryPriority == IslandPriority.SCREEN_RECORDING -> redRecordingOutline
        primaryPriority == IslandPriority.MEDIA -> mediaOutline
        else -> defaultTaskOutline
    }

    // Card 2 / Auxiliary Bubble (Secondary Island):
    // Secondary islands have the color of their respective task (15% opacity white usually, or red for recording) instead of the media color
    val secondaryOutlineColor = when {
        secondaryPriority == IslandPriority.SCREEN_RECORDING -> redRecordingOutline
        secondaryPriority == IslandPriority.MEDIA -> mediaOutline
        else -> defaultTaskOutline
    }

    val speed = settings.animationSpeed.coerceIn(0.5f, 2.0f)
    val speedSq = speed * speed

    // Animation Specs (dynamically scaled by settings.animationSpeed):
    val sizeAnimSpecDp = when {
        isCharging -> spring<androidx.compose.ui.unit.Dp>(
            dampingRatio = 0.60f,
            stiffness = 280f * speedSq
        )
        isExpanded -> spring<androidx.compose.ui.unit.Dp>(
            dampingRatio = 0.74f,
            stiffness = 380f * speedSq
        )
        else -> tween<androidx.compose.ui.unit.Dp>(
            durationMillis = (190 / speed).roundToInt(),
            easing = CubicBezierEasing(0.28f, 0.0f, 0.20f, 1.0f)
        )
    }

    val sizeAnimSpecFloat = when {
        isCharging -> spring<Float>(
            dampingRatio = 0.60f,
            stiffness = 280f * speedSq
        )
        isExpanded -> spring<Float>(
            dampingRatio = 0.74f,
            stiffness = 380f * speedSq
        )
        else -> tween<Float>(
            durationMillis = (190 / speed).roundToInt(),
            easing = CubicBezierEasing(0.28f, 0.0f, 0.20f, 1.0f)
        )
    }

    val positionAnimSpecDp = if (isExpanded) {
        spring<androidx.compose.ui.unit.Dp>(
            dampingRatio = 0.74f,
            stiffness = 380f * speedSq
        )
    } else {
        tween<androidx.compose.ui.unit.Dp>(
            durationMillis = (350 / speed).roundToInt(),
            easing = CubicBezierEasing(0.30f, 0.0f, 0.20f, 1.0f)
        )
    }

    // Dedicated spec for compact pill width:
    // When expanding: crisp spring shrinking into 0 width without overshoot
    // When emerging back: subtle horizontal bounce of a few pixels (dampingRatio = 0.63f)!
    val compactPillWidthAnimSpec = if (isExpanded) {
        spring<androidx.compose.ui.unit.Dp>(
            dampingRatio = 0.82f,
            stiffness = 380f * speedSq
        )
    } else {
        spring<androidx.compose.ui.unit.Dp>(
            dampingRatio = 0.63f,
            stiffness = 320f * speedSq
        )
    }

    val expandProgress by animateFloatAsState(
        targetValue = if (isExpanded && !isCharging) 1f else 0f,
        animationSpec = sizeAnimSpecFloat,
        label = "expandProgress"
    )

    val chargingContentAlpha by animateFloatAsState(
        targetValue = if (isCharging) 1f else 0f,
        animationSpec = tween(durationMillis = (130 / speed).roundToInt(), easing = FastOutSlowInEasing),
        label = "chargingContentAlpha"
    )

    val chargingPct = charging?.percentage ?: 0
    val targetChargingFraction = if (isCharging) (chargingPct.toFloat() / 100f).coerceIn(0f, 1f) else 0f
    val animatedChargingProgress by animateFloatAsState(
        targetValue = targetChargingFraction,
        animationSpec = if (isCharging) {
            tween(
                durationMillis = (750 / speed).roundToInt(),
                delayMillis = (60 / speed).roundToInt(),
                easing = FastOutSlowInEasing
            )
        } else {
            tween(
                durationMillis = (150 / speed).roundToInt(),
                easing = FastOutLinearInEasing
            )
        },
        label = "chargingProgress"
    )

    val chargingIslandWidth = (settings.widthDp + 20).dp.coerceAtLeast(140.dp)

    // Minimised Island Pill: sits docked at settings.topOffsetDp.dp.
    // When expanding, it animates from its compact width down to 0.dp.
    // When minimising, it animates from 0.dp back to its compact width.
    val targetCompactWidth = when {
        !isOccupied -> 0.dp
        isExpanded -> 0.dp // Shrinks horizontally into 0 width while card expands!
        isCharging -> chargingIslandWidth
        isDual -> (settings.widthDp * 0.74f).dp
        else -> settings.widthDp.dp
    }

    // Expanded Cards: Card 1 (and Card 2 if dual mode).
    // When expanded, Card 1 slides down to (settings.topOffsetDp + 52).dp, widening to 340.dp * scale.
    // When minimising, Card 1 shrinks back to 0.dp width as it slides back up.
    val targetCard1Width = if (isExpanded && !isCharging) {
        (340.dp * settings.expandedScale)
    } else {
        0.dp
    }

    val targetCard1Height = when {
        isExpanded && primaryPriority == IslandPriority.SCREEN_RECORDING -> (115.dp * settings.expandedScale)
        isExpanded -> (195.dp * settings.expandedScale)
        else -> settings.heightDp.dp
    }

    val targetCornerRadius = when {
        isExpanded && !isCharging -> 28.dp
        else -> settings.cornerRadiusDp.dp
    }

    // Card 2 delayed appearance: starts a split frame after Card 1 when expanding
    var card2Expanded by remember { mutableStateOf(false) }
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            kotlinx.coroutines.delay((45L / speed).toLong())
            card2Expanded = true
        } else {
            card2Expanded = false
        }
    }

    val showSecondaryCard = !isCharging && activeCount >= 2 && secondaryPriority != null
    val targetCard2Height = if (card2Expanded && showSecondaryCard) {
        val baseH = if (secondaryPriority == IslandPriority.SCREEN_RECORDING) 115.dp else 180.dp
        baseH * settings.expandedScale
    } else 0.dp
    val targetCard2Alpha = if (card2Expanded && showSecondaryCard) 1f else 0f
    val targetCard2OffsetY = if (card2Expanded && showSecondaryCard) 0.dp else (-14).dp

    val targetBubbleSize = when {
        !isCharging && isDual && !isExpanded -> settings.heightDp.dp
        else -> 0.dp
    }

    val targetGap = when {
        !isCharging && isDual && !isExpanded -> 8.dp
        else -> 0.dp
    }

    val targetTopOffset = if (isExpanded && !isCharging) {
        (settings.topOffsetDp + 52).dp // Sits way below initial position (comfortably below status bar) when expanded!
    } else {
        settings.topOffsetDp.dp // Docks right over the punch hole when minimised or charging
    }

    val targetCardSpacing = if (isExpanded && showSecondaryCard) 10.dp else 0.dp

    val animatedCompactWidth by animateDpAsState(targetValue = targetCompactWidth, animationSpec = compactPillWidthAnimSpec, label = "compactWidth")
    val animatedCard1Width by animateDpAsState(targetValue = targetCard1Width, animationSpec = sizeAnimSpecDp, label = "card1Width")
    val animatedCard1Height by animateDpAsState(targetValue = targetCard1Height, animationSpec = sizeAnimSpecDp, label = "card1Height")
    val animatedCornerRadius by animateDpAsState(targetValue = targetCornerRadius, animationSpec = sizeAnimSpecDp, label = "cornerRadius")
    val animatedCard2Height by animateDpAsState(targetValue = targetCard2Height, animationSpec = sizeAnimSpecDp, label = "card2Height")
    val animatedCard2Alpha by animateFloatAsState(targetValue = targetCard2Alpha, animationSpec = sizeAnimSpecFloat, label = "card2Alpha")
    val animatedCard2OffsetY by animateDpAsState(targetValue = targetCard2OffsetY, animationSpec = sizeAnimSpecDp, label = "card2OffsetY")
    val animatedBubbleSize by animateDpAsState(targetValue = targetBubbleSize, animationSpec = sizeAnimSpecDp, label = "bubbleSize")
    val animatedGap by animateDpAsState(targetValue = targetGap, animationSpec = sizeAnimSpecDp, label = "gap")
    val animatedTopOffset by animateDpAsState(targetValue = targetTopOffset, animationSpec = positionAnimSpecDp, label = "topOffset")
    val animatedCardSpacing by animateDpAsState(targetValue = targetCardSpacing, animationSpec = sizeAnimSpecDp, label = "cardSpacing")

    val cardContainerAlpha = if (isExpanded) 1f else ((expandProgress - 0.05f) / 0.95f).coerceIn(0f, 1f)

    var card1OffsetX by remember { mutableFloatStateOf(0f) }
    var card2OffsetX by remember { mutableFloatStateOf(0f) }

    val animatedCard1OffsetX by animateFloatAsState(
        targetValue = card1OffsetX,
        animationSpec = spring(dampingRatio = 0.76f, stiffness = 420f * speedSq),
        label = "card1OffsetX"
    )
    val animatedCard2OffsetX by animateFloatAsState(
        targetValue = card2OffsetX,
        animationSpec = spring(dampingRatio = 0.76f, stiffness = 420f * speedSq),
        label = "card2OffsetX"
    )

    LaunchedEffect(isExpanded) {
        if (!isExpanded) {
            card1OffsetX = 0f
            card2OffsetX = 0f
        }
    }

    LaunchedEffect(charging?.timestamp) {
        if (charging != null && settings.hapticsEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    LaunchedEffect(isExpanded, isOccupied, primaryPriority) {
        if (isExpanded) {
            if (!isOccupied || primaryPriority == IslandPriority.IDLE) {
                IslandStateManager.collapse()
            }
        }
    }

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

        // Expanded Cards Container: Positioned at animated top offset (slides down to (settings.topOffsetDp + 52).dp)
        if (expandProgress > 0.001f || isExpanded || animatedCard1Width > 1.dp) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = 0,
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
                    // Card 1: Primary Expanded Card
                    if (animatedCard1Width > 1.dp) {
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(animatedCard1OffsetX.roundToInt(), 0) }
                                .width(animatedCard1Width)
                                .height(animatedCard1Height)
                                .clipToBounds()
                                .graphicsLayer {
                                    alpha = cardContainerAlpha
                                }
                                .shadow(
                                    elevation = if (isExpanded) 20.dp else 0.dp,
                                    shape = RoundedCornerShape(animatedCornerRadius)
                                )
                                .clip(RoundedCornerShape(animatedCornerRadius))
                                .background(if (animatedCard1Width > 2.dp) Color.Black else Color.Transparent)
                                .then(
                                    if (settings.showOutline && animatedCard1Width > 8.dp) {
                                        Modifier.border(
                                            width = settings.outlineThicknessDp.dp,
                                            color = primaryOutlineColor,
                                            shape = RoundedCornerShape(animatedCornerRadius)
                                        )
                                    } else Modifier
                                )
                                .then(
                                    if (isExpanded) {
                                        Modifier.pointerInput(primaryPriority) {
                                            detectHorizontalDragGestures(
                                                onDragEnd = {
                                                    val threshold = 160f
                                                    when (primaryPriority) {
                                                        IslandPriority.MEDIA -> {
                                                            if (abs(card1OffsetX) > threshold) {
                                                                triggerHaptic()
                                                                IslandStateManager.dismissMedia()
                                                                if (activeCount <= 1) {
                                                                    IslandStateManager.collapse()
                                                                }
                                                            }
                                                        }
                                                        IslandPriority.POP_NOTIFICATION,
                                                        IslandPriority.PROGRESS -> {
                                                            if (abs(card1OffsetX) > threshold) {
                                                                triggerHaptic()
                                                                val dismissed = IslandStateManager.dismissCurrentNotification()
                                                                if (dismissed && activeCount <= 1) {
                                                                    IslandStateManager.collapse()
                                                                }
                                                            }
                                                        }
                                                        else -> Unit
                                                    }
                                                    card1OffsetX = 0f
                                                },
                                                onDragCancel = {
                                                    card1OffsetX = 0f
                                                },
                                                onHorizontalDrag = { _, dragAmount ->
                                                    when (primaryPriority) {
                                                        IslandPriority.SCREEN_RECORDING -> {
                                                            card1OffsetX += dragAmount * 0.18f
                                                        }
                                                        else -> {
                                                            card1OffsetX += dragAmount
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // Subtle ambient gradient background ONLY in the expanded media player card
                            if (isExpanded && primaryPriority == IslandPriority.MEDIA && mediaAccentColor != Color.Transparent) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    mediaAccentColor.copy(alpha = 0.35f * expandProgress),
                                                    mediaAccentColor.copy(alpha = 0.12f * expandProgress),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                )
                            }

                            // Expanded Card 1 Content (e.g. Media Player or Primary Notification)
                            val expandedAlpha = if (isExpanded) {
                                ((expandProgress - 0.30f) / 0.70f).coerceIn(0f, 1f)
                            } else {
                                ((expandProgress - 0.45f) / 0.55f).coerceIn(0f, 1f)
                            }

                            if (!isCharging && expandedAlpha > 0.01f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(20.dp)
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
                    }

                    // Card 2: Secondary Action Card (smoothly expands and collapses in dual mode!)
                    if (animatedCard2Height > 1.dp) {
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(animatedCard2OffsetX.roundToInt(), animatedCard2OffsetY.roundToPx()) }
                                .width(animatedCard1Width)
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
                                            color = secondaryOutlineColor,
                                            shape = RoundedCornerShape(animatedCornerRadius)
                                        )
                                    } else Modifier
                                )
                                .then(
                                    if (isExpanded && secondaryPriority != null) {
                                        Modifier.pointerInput(secondaryPriority) {
                                            detectHorizontalDragGestures(
                                                onDragEnd = {
                                                    val threshold = 160f
                                                    when (secondaryPriority) {
                                                        IslandPriority.MEDIA -> {
                                                            if (abs(card2OffsetX) > threshold) {
                                                                triggerHaptic()
                                                                IslandStateManager.dismissMedia()
                                                            }
                                                        }
                                                        IslandPriority.POP_NOTIFICATION,
                                                        IslandPriority.PROGRESS -> {
                                                            if (abs(card2OffsetX) > threshold) {
                                                                triggerHaptic()
                                                                IslandStateManager.dismissCurrentNotification()
                                                            }
                                                        }
                                                        else -> Unit
                                                    }
                                                    card2OffsetX = 0f
                                                },
                                                onDragCancel = {
                                                    card2OffsetX = 0f
                                                },
                                                onHorizontalDrag = { _, dragAmount ->
                                                    when (secondaryPriority) {
                                                        IslandPriority.PROGRESS,
                                                        IslandPriority.SCREEN_RECORDING -> {
                                                            card2OffsetX += dragAmount * 0.18f
                                                        }
                                                        else -> {
                                                            card2OffsetX += dragAmount
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    } else Modifier
                                )
                                .padding(20.dp)
                        ) {
                            // Subtle ambient gradient background for secondary card if it's the media player
                            if (isExpanded && secondaryPriority == IslandPriority.MEDIA && mediaAccentColor != Color.Transparent) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    mediaAccentColor.copy(alpha = 0.35f * animatedCard2Alpha),
                                                    mediaAccentColor.copy(alpha = 0.12f * animatedCard2Alpha),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                )
                            }

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
            }
        }

        // Minimised Island Container: Positioned strictly at camera cutout level (settings.topOffsetDp.dp)
        // Shrinks horizontally to 0 width while the card expands downwards, and blossoms from 0 width when minimising!
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = 0,
                        y = settings.topOffsetDp.dp.roundToPx()
                    )
                },
            contentAlignment = Alignment.TopCenter
        ) {
            // Main Compact Pill (Shrinks to 0 width on expand, expands from 0 width on collapse)
            if (animatedCompactWidth > 1.dp) {
                // Smoothly fade away over the last few frames before vanishing into the punch hole, and fade in on emergence
                val compactPillAlpha = ((animatedCompactWidth - 6.dp) / 22.dp).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .width(animatedCompactWidth)
                        .height(settings.heightDp.dp)
                        .clipToBounds()
                        .graphicsLayer { alpha = compactPillAlpha }
                        .shadow(
                            elevation = (if (isOccupied) 8.dp else 0.dp) * compactPillAlpha,
                            shape = RoundedCornerShape(settings.cornerRadiusDp.dp)
                        )
                        .clip(RoundedCornerShape(settings.cornerRadiusDp.dp))
                        .background(if (animatedCompactWidth > 2.dp) Color.Black else Color.Transparent)
                        .then(
                            if (settings.showOutline && animatedCompactWidth > 8.dp) {
                                Modifier.border(
                                    width = settings.outlineThicknessDp.dp,
                                    color = primaryOutlineColor,
                                    shape = RoundedCornerShape(settings.cornerRadiusDp.dp)
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
                    val compactAlpha = if (isExpanded) {
                        (1f - (expandProgress * 3.0f)).coerceIn(0f, 1f)
                    } else {
                        (1f - (expandProgress * 1.8f)).coerceIn(0f, 1f)
                    }

                    // Background darker green progress bar for charging status:
                    if (isCharging || chargingContentAlpha > 0.01f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = chargingContentAlpha }
                                .background(Color(0xFF04190B))
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = animatedChargingProgress.coerceIn(0.001f, 1f))
                                .align(Alignment.CenterStart)
                                .graphicsLayer { alpha = chargingContentAlpha }
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF062B12),
                                            Color(0xFF0D3D1B)
                                        )
                                    )
                                )
                        )
                    }

                    // 1. Compact Content or Charging Content: strictly clear 36dp center over punch-hole
                    if (isCharging || chargingContentAlpha > 0.01f) {
                        val wattage = charging?.wattage
                        val chargingLabel = if (wattage != null && wattage >= 1.0) {
                            if (wattage >= 10.0 || (wattage % 1.0) < 0.05 || (wattage % 1.0) > 0.95) {
                                "${wattage.roundToInt()}W"
                            } else {
                                String.format(Locale.US, "%.1fW", wattage)
                            }
                        } else {
                            "Charging"
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp)
                                .graphicsLayer { alpha = chargingContentAlpha },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Wattage (e.g. "33W") or "Charging" to save extreme space
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = chargingLabel,
                                    color = Color(0xFF30D158),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Center: 36dp deadzone strictly over camera cutout
                            Spacer(modifier = Modifier.width(36.dp))

                            // Right: Current battery percentage in green
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Text(
                                    text = "${charging?.percentage ?: 100}%",
                                    color = Color(0xFF30D158),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    } else if (compactAlpha > 0.01f && animatedCompactWidth > 24.dp) {
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
                }
            }

            // Auxiliary Circular Island Bubble: Placed strictly to the RIGHT of the compact pill
            // Never overlaps or comes near the center punch-hole, and does NOT fly across from the expanded card!
            if (animatedBubbleSize > 3.dp && isDual) {
                val compactMainHalf = (settings.widthDp * 0.74f).dp / 2f
                val bubbleOffsetXPx = with(LocalDensity.current) {
                    (compactMainHalf + animatedGap + (animatedBubbleSize / 2)).roundToPx()
                }

                // Bubble only fades in once island is almost back to compact mode (never flying across mid-collapse!)
                val bubbleAlpha = if (isExpanded) 0f else ((1f - expandProgress * 3.5f).coerceIn(0f, 1f))

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
                                    color = secondaryOutlineColor,
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

            // Parentheses Accents: Neat parentheses-shaped brackets on each side of the island
            // Shorter than the height of the island, smoothly positioning symmetrically around 1 or 2 activities, and neatly close to the edges.
            val targetParenthesisAlpha = if (!isOccupied) 0f else ((1f - expandProgress * 3.5f).coerceIn(0f, 1f))
            val parenthesisAlphaAnim by animateFloatAsState(
                targetValue = targetParenthesisAlpha,
                animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
                label = "parenthesisAlpha"
            )
            if (parenthesisAlphaAnim > 0.01f && animatedCompactWidth > 20.dp) {
                val parenthesisHeight = (settings.heightDp.dp * 0.50f).coerceIn(15.dp, 20.dp)
                val parenthesisWidth = 4.5.dp
                val parenthesisGap = 3.5.dp
                val parenthesisStroke = 1.8.dp
                val density = LocalDensity.current

                val pYPx = with(density) { ((settings.heightDp.dp - parenthesisHeight) / 2f).roundToPx() }
                val tipOffsetFromCenter = (parenthesisWidth - parenthesisStroke) / 2f

                // Left parenthesis `(`: tips facing right are at centerX + tipOffsetFromCenter.
                // We want tips to sit exactly at islandLeft - parenthesisGap.
                val islandLeftDp = -animatedCompactWidth / 2f
                val leftCenterXPx = with(density) {
                    (islandLeftDp - parenthesisGap - tipOffsetFromCenter).roundToPx()
                }

                // Right parenthesis `)`: tips facing left are at centerX - tipOffsetFromCenter.
                // We want tips to sit exactly at rightContentEdgeDp + parenthesisGap.
                val rightContentEdgeDp = if (!isCharging && isDual && animatedBubbleSize > 2.dp) {
                    val compactMainHalf = (settings.widthDp * 0.74f).dp / 2f
                    (compactMainHalf + animatedGap + animatedBubbleSize).coerceAtLeast(animatedCompactWidth / 2f)
                } else {
                    animatedCompactWidth / 2f
                }
                val rightCenterXPx = with(density) {
                    (rightContentEdgeDp + parenthesisGap + tipOffsetFromCenter).roundToPx()
                }

                val bracketColor = Color.Black

                // Left Parenthesis `(`
                ParenthesisBracket(
                    isLeft = true,
                    width = parenthesisWidth,
                    height = parenthesisHeight,
                    strokeWidth = parenthesisStroke,
                    color = bracketColor,
                    modifier = Modifier
                        .offset { IntOffset(x = leftCenterXPx, y = pYPx) }
                        .graphicsLayer { alpha = parenthesisAlphaAnim }
                )

                // Right Parenthesis `)`
                ParenthesisBracket(
                    isLeft = false,
                    width = parenthesisWidth,
                    height = parenthesisHeight,
                    strokeWidth = parenthesisStroke,
                    color = bracketColor,
                    modifier = Modifier
                        .offset { IntOffset(x = rightCenterXPx, y = pYPx) }
                        .graphicsLayer { alpha = parenthesisAlphaAnim }
                )
            }

            // Extended hit-box for compact mode: covers the pill + extends 36dp downwards into the safe zone below the status bar
            if (!isExpanded && isOccupied) {
                val compactHitWidth = if (isDual) {
                    val mainHalf = (settings.widthDp * 0.74f).dp / 2f
                    (mainHalf + animatedGap + animatedBubbleSize + 24.dp) * 2f
                } else {
                    animatedCompactWidth + 36.dp
                }
                Box(
                    modifier = Modifier
                        .width(compactHitWidth)
                        .height(settings.heightDp.dp + 36.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            triggerHaptic()
                            IslandStateManager.expand()
                        }
                )
            }
        }
    }
}

@Composable
fun ParenthesisBracket(
    isLeft: Boolean,
    width: Dp,
    height: Dp,
    strokeWidth: Dp,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(width, height)) {
        val w = size.width
        val h = size.height
        val strokePx = strokeWidth.toPx()
        val path = Path()

        if (isLeft) {
            // Left parenthesis `(`: curves outward to the left
            path.moveTo(w - strokePx / 2f, strokePx / 2f)
            path.cubicTo(
                x1 = strokePx / 2f, y1 = h * 0.22f,
                x2 = strokePx / 2f, y2 = h * 0.78f,
                x3 = w - strokePx / 2f, y3 = h - strokePx / 2f
            )
        } else {
            // Right parenthesis `)`: curves outward to the right
            path.moveTo(strokePx / 2f, strokePx / 2f)
            path.cubicTo(
                x1 = w - strokePx / 2f, y1 = h * 0.22f,
                x2 = w - strokePx / 2f, y2 = h * 0.78f,
                x3 = strokePx / 2f, y3 = h - strokePx / 2f
            )
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokePx,
                cap = StrokeCap.Round
            )
        )
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
        IslandPriority.PROGRESS -> {
            val pct = notification?.progressFraction ?: 0f
            val isIndet = notification?.isIndeterminate == true
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isIndet) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFFE5E5EA),
                        trackColor = Color(0xFF2C2C2E)
                    )
                } else {
                    CircularProgressIndicator(
                        progress = { pct },
                        modifier = Modifier.size(17.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFFE5E5EA),
                        trackColor = Color(0xFF2C2C2E)
                    )
                    Text(
                        text = "${(pct * 100).roundToInt()}",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        IslandPriority.MEDIA -> {
            val displayArt = media?.albumArt ?: media?.appIcon
            if (displayArt != null) {
                Image(
                    bitmap = displayArt.asImageBitmap(),
                    contentDescription = "Media Art",
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
                    PulsingRecordingDot()
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

                IslandPriority.PROGRESS -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (notification?.iconBitmap != null) {
                            Image(
                                bitmap = notification.iconBitmap.asImageBitmap(),
                                contentDescription = "Download Icon",
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = Color(0xFFE5E5EA),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (!isCompactDual) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = notification?.appName ?: "Download",
                                color = Color(0xFFE5E5EA),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                IslandPriority.MEDIA -> {
                    val displayArt = media?.albumArt ?: media?.appIcon
                    if (displayArt != null) {
                        Image(
                            bitmap = displayArt.asImageBitmap(),
                            contentDescription = "Media Art",
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(5.dp))
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color(0xFFA1A1AA),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IslandPriority.IDLE -> Unit
            }
        }

        // Camera punch-hole deadzone: STRICTLY 36dp EMPTY SPACE IN THE MIDDLE!
        // No UI elements will EVER enter or overlap the camera cutout area
        Spacer(modifier = Modifier.width(36.dp))

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

                IslandPriority.PROGRESS -> {
                    val pct = notification?.progressFraction ?: 0f
                    val isIndet = notification?.isIndeterminate == true
                    if (isIndet) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFE5E5EA),
                            trackColor = Color(0xFF2C2C2E)
                        )
                    } else {
                        Text(
                            text = "${(pct * 100).roundToInt()}%",
                            color = Color(0xFFE5E5EA),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                IslandPriority.MEDIA -> {
                    val displayArt = media?.albumArt ?: media?.appIcon
                    val compactAccent = remember(displayArt) { extractMediaAccentColor(displayArt) }
                    val compactEqTint = if (compactAccent != Color.Transparent) {
                        lerp(Color(0xFFE5E5EA), compactAccent, 0.50f)
                    } else {
                        Color(0xFFE5E5EA)
                    }
                    EqualizerVisualizer(isPlaying = media?.isPlaying == true, barColor = compactEqTint)
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
        IslandPriority.PROGRESS -> {
            ExpandedProgressView(notification, onCollapse)
        }
        IslandPriority.MEDIA -> {
            ExpandedMediaView(media, onCollapse)
        }
        IslandPriority.IDLE -> {
            LaunchedEffect(Unit) {
                onCollapse()
            }
            ExpandedIdleView(onCollapse)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedMediaView(media: MediaInfo?, onCollapse: () -> Unit) {
    val context = LocalContext.current
    var isSeeking by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableFloatStateOf(0f) }

    val currentPosition = if (isSeeking) scrubPosition.toLong() else (media?.positionMs ?: 0L)
    val duration = (media?.durationMs ?: 0L).coerceAtLeast(1L)

    val displayArt = media?.albumArt ?: media?.appIcon
    val mediaAccentColor = remember(displayArt) { extractMediaAccentColor(displayArt) }

    val controlsTint = if (mediaAccentColor != Color.Transparent) {
        lerp(Color(0xFFE5E5EA), mediaAccentColor, 0.40f)
    } else {
        Color(0xFFE5E5EA)
    }

    val playButtonBg = if (mediaAccentColor != Color.Transparent) {
        lerp(Color.White, mediaAccentColor, 0.28f)
    } else {
        Color.White
    }

    val playButtonIconTint = if (mediaAccentColor != Color.Transparent) {
        lerp(Color.Black, mediaAccentColor, 0.15f)
    } else {
        Color.Black
    }

    val eqTint = if (mediaAccentColor != Color.Transparent) {
        lerp(Color(0xFFE5E5EA), mediaAccentColor, 0.50f)
    } else {
        Color(0xFFE5E5EA)
    }

    val sliderActiveTint = if (mediaAccentColor != Color.Transparent) {
        lerp(Color(0xFFE5E5EA), mediaAccentColor, 0.55f)
    } else {
        Color(0xFFE5E5EA)
    }

    val sliderInactiveTint = if (mediaAccentColor != Color.Transparent) {
        lerp(Color(0xFF2C2C2E), mediaAccentColor, 0.22f)
    } else {
        Color(0xFF2C2C2E)
    }

    val durationTextTint = if (mediaAccentColor != Color.Transparent) {
        lerp(Color(0xFF8E8E93), mediaAccentColor, 0.35f)
    } else {
        Color(0xFF8E8E93)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header: Album cover / app icon + Title & Artist + Equalizer
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album cover (larger, tap to open app and collapse island)
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF212121))
                    .clickable {
                        runCatching {
                            if (media?.sessionActivity != null) {
                                media.sessionActivity.send()
                            } else if (!media?.packageName.isNullOrBlank()) {
                                val launchIntent = context.packageManager.getLaunchIntentForPackage(media.packageName)
                                if (launchIntent != null) {
                                    context.startActivity(launchIntent)
                                }
                            }
                        }
                        onCollapse()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (displayArt != null) {
                    Image(
                        bitmap = displayArt.asImageBitmap(),
                        contentDescription = "Cover Art",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = null,
                        tint = Color(0xFFA1A1AA),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Title with infinite marquee slide
                Text(
                    text = media?.title?.ifBlank { "No Media Playing" } ?: "No Media Playing",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        initialDelayMillis = 1000,
                        velocity = 30.dp
                    )
                )
                Spacer(modifier = Modifier.height(3.dp))
                // Artist with marquee slide
                Text(
                    text = media?.artist?.ifBlank { "Unknown Artist" } ?: "Unknown Artist",
                    color = Color(0xFFA1A1AA),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        initialDelayMillis = 1500,
                        velocity = 26.dp
                    )
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            EqualizerVisualizer(isPlaying = media?.isPlaying == true, barColor = eqTint)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Progress Bar & Duration on Left & Right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatDuration(currentPosition),
                color = durationTextTint,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.width(10.dp))

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
                thumb = {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(sliderActiveTint)
                    )
                },
                track = { sliderState ->
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        drawStopIndicator = null,
                        drawTick = { _, _ -> },
                        colors = SliderDefaults.colors(
                            activeTrackColor = sliderActiveTint,
                            inactiveTrackColor = sliderInactiveTint
                        )
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(20.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = formatDuration(duration),
                color = durationTextTint,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Playback Controls (moved upward, no big gap)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { IslandStateManager.onSkipPrevious?.invoke() },
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FastRewind,
                    contentDescription = "Previous",
                    tint = controlsTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            IconButton(
                onClick = { IslandStateManager.onPlayPause?.invoke() },
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(playButtonBg)
            ) {
                Icon(
                    imageVector = if (media?.isPlaying == true) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = playButtonIconTint,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            IconButton(
                onClick = { IslandStateManager.onSkipNext?.invoke() },
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FastForward,
                    contentDescription = "Next",
                    tint = controlsTint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun ExpandedNotificationView(notification: NotificationInfo?, onCollapse: () -> Unit) {
    val context = LocalContext.current
    var isReplying by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }

    val replyAction = remember(notification) {
        notification?.actions?.firstOrNull { it.isReply || it.title.contains("Reply", ignoreCase = true) }
    }

    fun openNotification() {
        IslandStateManager.setKeyboardFocused(false)
        val pendingIntent = notification?.contentIntent
        var launched = false
        if (pendingIntent != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val options = ActivityOptions.makeBasic().apply {
                        pendingIntentBackgroundActivityStartMode = ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                    }.toBundle()
                    pendingIntent.send(context, 0, null, null, null, null, options)
                } else {
                    pendingIntent.send()
                }
                launched = true
            } catch (e: Exception) {
                Log.e("ExpandedNotification", "Failed to send contentIntent", e)
            }
        }
        if (!launched && !notification?.packageName.isNullOrBlank()) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(notification.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            }
        }
        onCollapse()
    }

    fun sendReply(text: String) {
        if (text.isBlank() || notification == null) return
        IslandStateManager.onSendReply?.invoke(notification.key, text.trim())
        IslandStateManager.setKeyboardFocused(false)
        IslandStateManager.dismissCurrentNotification()
        onCollapse()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top section: Header + Body text
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (notification?.iconBitmap != null) {
                    Image(
                        bitmap = notification.iconBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF2C2C2E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color(0xFFE5E5EA),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = notification?.title ?: "Notification",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                    )
                    Text(
                        text = notification?.appName ?: "",
                        color = Color(0xFFA1A1AA),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = {
                        IslandStateManager.setKeyboardFocused(false)
                        IslandStateManager.dismissCurrentNotification()
                        onCollapse()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color(0xFF8E8E93),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = notification?.text ?: "",
                color = Color(0xFFF4F4F5),
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { openNotification() }
            )
        }

        // Action Section: Either Inline Reply Row or Action Buttons
        if (isReplying) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Smart reply choices chips
                val smartChoices = remember(replyAction) {
                    val choices = replyAction?.remoteInputChoices ?: emptyList()
                    if (choices.isNotEmpty()) choices else listOf("OK", "Sure", "Thanks!")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    smartChoices.take(3).forEach { choice ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF2C2C2E))
                                .clickable { sendReply(choice) }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = choice,
                                color = Color(0xFFE5E5EA),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Inline Reply Text Input Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(Color(0xFF212121))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    IslandStateManager.setKeyboardFocused(true)
                                }
                            },
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 14.sp
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(Color(0xFFE5E5EA)),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { sendReply(replyText) }),
                        decorationBox = { innerTextField ->
                            if (replyText.isEmpty()) {
                                Text(
                                    text = replyAction?.remoteInputLabel ?: "Type a reply...",
                                    color = Color(0xFF71717A),
                                    fontSize = 14.sp
                                )
                            }
                            innerTextField()
                        }
                    )

                    IconButton(
                        onClick = { sendReply(replyText) },
                        enabled = replyText.isNotBlank(),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send Reply",
                            tint = if (replyText.isNotBlank()) Color.White else Color(0xFF52525B),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            isReplying = false
                            IslandStateManager.setKeyboardFocused(false)
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = Color(0xFF71717A),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        } else {
            // Action Buttons: Taller, dynamically dividing available width equally, spaced by 4dp,
            // with inner corners facing adjacent buttons being squarish
            val actionItems = remember(notification, replyAction) {
                val list = mutableListOf<NotificationActionItem>()
                val notifActions = notification?.actions ?: emptyList()
                notifActions.take(2).forEach { action ->
                    val isThisReply = action == replyAction || action.isReply || action.title.contains("Reply", ignoreCase = true)
                    list.add(
                        NotificationActionItem(
                            title = action.title,
                            isPrimary = false,
                            onClick = {
                                if (isThisReply) {
                                    isReplying = true
                                    IslandStateManager.setKeyboardFocused(true)
                                } else {
                                    runCatching { action.actionIntent?.send() }
                                    IslandStateManager.dismissCurrentNotification()
                                    onCollapse()
                                }
                            }
                        )
                    )
                }
                list.add(
                    NotificationActionItem(
                        title = "Open",
                        isPrimary = true,
                        onClick = { openNotification() }
                    )
                )
                list
            }

            val totalButtons = actionItems.size
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                actionItems.forEachIndexed { index, item ->
                    val shape = when {
                        totalButtons <= 1 -> RoundedCornerShape(14.dp)
                        index == 0 -> RoundedCornerShape(
                            topStart = 14.dp,
                            bottomStart = 14.dp,
                            topEnd = 4.dp,
                            bottomEnd = 4.dp
                        )
                        index == totalButtons - 1 -> RoundedCornerShape(
                            topStart = 4.dp,
                            bottomStart = 4.dp,
                            topEnd = 14.dp,
                            bottomEnd = 14.dp
                        )
                        else -> RoundedCornerShape(4.dp)
                    }

                    FilledTonalButton(
                        onClick = item.onClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = shape,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (item.isPrimary) Color(0xFFE5E5EA) else Color(0xFF2C2C2E),
                            contentColor = if (item.isPrimary) Color.Black else Color(0xFFF4F4F5)
                        )
                    ) {
                        Text(
                            text = item.title,
                            fontSize = 13.5.sp,
                            fontWeight = if (item.isPrimary) FontWeight.Bold else FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private data class NotificationActionItem(
    val title: String,
    val isPrimary: Boolean,
    val onClick: () -> Unit
)

@Composable
fun ExpandedProgressView(notification: NotificationInfo?, onCollapse: () -> Unit) {
    val context = LocalContext.current
    val progressFraction = notification?.progressFraction ?: 0f
    val isIndet = notification?.isIndeterminate == true
    val percentageText = if (isIndet) "" else "${(progressFraction * 100).roundToInt()}%"

    fun openProgressApp() {
        runCatching {
            if (notification?.contentIntent != null) {
                notification.contentIntent.send()
            } else if (!notification?.packageName.isNullOrBlank()) {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(notification.packageName)
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                }
            }
        }
        onCollapse()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top section: Header + Detail text + Progress bar
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (notification?.iconBitmap != null) {
                    Image(
                        bitmap = notification.iconBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF212121)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Color(0xFFE5E5EA),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = notification?.title?.ifBlank { "Downloading" } ?: "Downloading",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                    )
                    Text(
                        text = notification?.appName ?: "",
                        color = Color(0xFFA1A1AA),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!isIndet) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF2C2C2E)
                    ) {
                        Text(
                            text = percentageText,
                            color = Color(0xFFE5E5EA),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.5.dp,
                        color = Color(0xFFE5E5EA),
                        trackColor = Color(0xFF2C2C2E)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = {
                        IslandStateManager.dismissCurrentNotification()
                        onCollapse()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color(0xFF8E8E93),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val rawProgressText = notification?.text ?: "In progress..."
            val displayProgressText = remember(rawProgressText) {
                var s = rawProgressText.trim()
                while (s.startsWith("•") || s.startsWith("·") || s.startsWith("-")) {
                    s = s.substring(1).trim()
                }
                while (s.endsWith("•") || s.endsWith("·") || s.endsWith("-")) {
                    s = s.substring(0, s.length - 1).trim()
                }
                s.ifBlank { "In progress..." }
            }

            Text(
                text = displayProgressText,
                color = Color(0xFFF4F4F5),
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { openProgressApp() }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Linear Progress Bar
            if (isIndet) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFFE5E5EA),
                    trackColor = Color(0xFF2C2C2E)
                )
            } else {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFFE5E5EA),
                    trackColor = Color(0xFF2C2C2E)
                )
            }
        }

        // Action Buttons: Taller, dynamically dividing available width equally, spaced by 4dp,
        // with inner corners facing adjacent buttons being squarish
        val actionItems = remember(notification) {
            val list = mutableListOf<NotificationActionItem>()
            val notifActions = notification?.actions ?: emptyList()
            notifActions.take(2).forEach { action ->
                list.add(
                    NotificationActionItem(
                        title = action.title,
                        isPrimary = false,
                        onClick = {
                            runCatching { action.actionIntent?.send() }
                        }
                    )
                )
            }
            list.add(
                NotificationActionItem(
                    title = "Open",
                    isPrimary = true,
                    onClick = { openProgressApp() }
                )
            )
            list
        }

        val totalButtons = actionItems.size
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            actionItems.forEachIndexed { index, item ->
                val shape = when {
                    totalButtons <= 1 -> RoundedCornerShape(14.dp)
                    index == 0 -> RoundedCornerShape(
                        topStart = 14.dp,
                        bottomStart = 14.dp,
                        topEnd = 4.dp,
                        bottomEnd = 4.dp
                    )
                    index == totalButtons - 1 -> RoundedCornerShape(
                        topStart = 4.dp,
                        bottomStart = 4.dp,
                        topEnd = 14.dp,
                        bottomEnd = 14.dp
                    )
                    else -> RoundedCornerShape(4.dp)
                }

                FilledTonalButton(
                    onClick = item.onClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = shape,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (item.isPrimary) Color(0xFFE5E5EA) else Color(0xFF2C2C2E),
                        contentColor = if (item.isPrimary) Color.Black else Color(0xFFF4F4F5)
                    )
                ) {
                    Text(
                        text = item.title,
                        fontSize = 13.5.sp,
                        fontWeight = if (item.isPrimary) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
                    text = "Screen recording",
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val leftShape = RoundedCornerShape(
                topStart = 14.dp,
                bottomStart = 14.dp,
                topEnd = 4.dp,
                bottomEnd = 4.dp
            )
            FilledTonalButton(
                onClick = {
                    IslandStateManager.onTogglePauseRecording?.invoke()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                shape = leftShape,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFF2C2C2E),
                    contentColor = Color(0xFFF4F4F5)
                )
            ) {
                Icon(
                    imageVector = if (recording.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (recording.isPaused) "Resume" else "Pause",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            val rightShape = RoundedCornerShape(
                topStart = 4.dp,
                bottomStart = 4.dp,
                topEnd = 14.dp,
                bottomEnd = 14.dp
            )
            FilledTonalButton(
                onClick = {
                    IslandStateManager.onStopRecording?.invoke()
                    onCollapse()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                shape = rightShape,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFF2C2C2E),
                    contentColor = Color(0xFFFF453A)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = null,
                    tint = Color(0xFFFF453A),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Stop",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF453A)
                )
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
fun PulsingRecordingDot(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier
            .size(11.dp)
            .scale(1f + (alpha * 0.12f))
            .clip(CircleShape)
            .background(Color(0xFFFF3B30).copy(alpha = alpha))
    )
}

@Composable
fun EqualizerVisualizer(isPlaying: Boolean, barColor: Color = Color(0xFFE5E5EA)) {
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
                .background(barColor)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height((16 * currentH2).dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(barColor)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height((16 * currentH3).dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(barColor)
        )
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
