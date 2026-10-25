package com.droidbubble.app.ui.main

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import java.util.Locale
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.ScreenSearchDesktop
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.service.IslandOverlayService
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScreen(
    viewModel: MainScreenViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val settings by viewModel.settings.collectAsState()
    val pendingPermissions by viewModel.pendingPermissions.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val isLoadingApps by viewModel.isLoadingApps.collectAsState()

    var showAppFilterSheet by remember { mutableStateOf(false) }
    var appFilterQuery by remember { mutableStateOf("") }

    // Re-check permissions on every onResume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissions(context)
                if (settings.isEnabled && PermissionHelper.isOverlayGranted(context)) {
                    IslandOverlayService.start(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SECTION 1: PERMISSIONS (Hides individual item once granted, hides whole section when all granted!)
            if (pendingPermissions.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Required Setup",
                        icon = Icons.Default.Security,
                        badge = "${pendingPermissions.size} Pending",
                        badgeColor = MaterialTheme.colorScheme.error
                    )
                }

                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        pendingPermissions.forEachIndexed { index, perm ->
                            val position = when {
                                pendingPermissions.size == 1 -> GroupPosition.SINGLE
                                index == 0 -> GroupPosition.TOP
                                index == pendingPermissions.size - 1 -> GroupPosition.BOTTOM
                                else -> GroupPosition.MIDDLE
                            }
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = getGroupedShape(position),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                            ) {
                                ListItem(
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    headlineContent = {
                                        Text(perm.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    },
                                    leadingContent = {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.errorContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    },
                                    trailingContent = {
                                        Button(
                                            onClick = {
                                                runCatching { context.startActivity(perm.createIntent(context)) }
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Grant", fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // SECTION 2: MASTER ENABLE SWITCH
            item {
                SettingsCardItem(position = GroupPosition.SINGLE) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text("Enable Island", fontWeight = FontWeight.SemiBold) },
                        leadingContent = {
                            Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingContent = {
                            Switch(
                                checked = settings.isEnabled,
                                onCheckedChange = { enabled ->
                                    viewModel.toggleEnabled(context, enabled)
                                }
                            )
                        }
                    )
                }
            }

            // SECTION 3: GEOMETRY SLIDERS (Separated items, 4.dp gap, inner square-ish outer rounded)
            item {
                SectionHeader(
                    title = "Island Dimensions",
                    icon = Icons.Default.Straighten
                )
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SettingsCardItem(position = GroupPosition.TOP) {
                        SliderSettingItem(
                            label = "Width",
                            value = settings.widthDp.toFloat(),
                            unit = "dp",
                            valueRange = 50f..260f,
                            onValueChange = { viewModel.updateSettings { s -> s.copy(widthDp = it.roundToInt()) } }
                        )
                    }

                    SettingsCardItem(position = GroupPosition.MIDDLE) {
                        SliderSettingItem(
                            label = "Height",
                            value = settings.heightDp.toFloat(),
                            unit = "dp",
                            valueRange = 26f..56f,
                            onValueChange = { viewModel.updateSettings { s -> s.copy(heightDp = it.roundToInt()) } }
                        )
                    }

                    SettingsCardItem(position = GroupPosition.MIDDLE) {
                        SliderSettingItem(
                            label = "Corner Radius",
                            value = settings.cornerRadiusDp.toFloat(),
                            unit = "dp",
                            valueRange = 10f..28f,
                            onValueChange = { viewModel.updateSettings { s -> s.copy(cornerRadiusDp = it.roundToInt()) } }
                        )
                    }

                    SettingsCardItem(position = GroupPosition.MIDDLE) {
                        SliderSettingItem(
                            label = "Top Offset (Notch Line-up)",
                            value = settings.topOffsetDp.toFloat(),
                            unit = "dp",
                            valueRange = 0f..60f,
                            onValueChange = { viewModel.updateSettings { s -> s.copy(topOffsetDp = it.roundToInt()) } }
                        )
                    }

                    SettingsCardItem(position = GroupPosition.MIDDLE) {
                        SliderSettingItem(
                            label = "Horizontal Offset",
                            value = settings.horizontalOffsetDp.toFloat(),
                            unit = "dp",
                            valueRange = -50f..50f,
                            onValueChange = { viewModel.updateSettings { s -> s.copy(horizontalOffsetDp = it.roundToInt()) } }
                        )
                    }

                    SettingsCardItem(position = GroupPosition.BOTTOM) {
                        SliderSettingItem(
                            label = "Expanded Card Scale",
                            value = settings.expandedScale,
                            unit = "x",
                            valueRange = 0.8f..1.3f,
                            steps = 5,
                            onValueChange = { viewModel.updateSettings { s -> s.copy(expandedScale = (it * 10).roundToInt() / 10f) } }
                        )
                    }
                }
            }

            // SECTION 4: APPEARANCE & FEEDBACK
            item {
                SectionHeader(
                    title = "Appearance & Feedback",
                    icon = Icons.Default.Palette
                )
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val outlineActive = settings.showOutline
                    SettingsCardItem(position = if (outlineActive) GroupPosition.TOP else GroupPosition.SINGLE) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Island Outline", fontWeight = FontWeight.SemiBold) },
                            leadingContent = {
                                Icon(Icons.Default.CropSquare, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            },
                            trailingContent = {
                                Switch(
                                    checked = settings.showOutline,
                                    onCheckedChange = { show ->
                                        viewModel.updateSettings { it.copy(showOutline = show) }
                                    }
                                )
                            }
                        )
                    }

                    if (outlineActive) {
                        SettingsCardItem(position = GroupPosition.BOTTOM) {
                            SliderSettingItem(
                                label = "Outline Thickness",
                                value = settings.outlineThicknessDp,
                                unit = "dp",
                                valueRange = 0.5f..4.0f,
                                onValueChange = { viewModel.updateSettings { s -> s.copy(outlineThicknessDp = (it * 10).roundToInt() / 10f) } }
                            )
                        }
                    }
                }
            }

            item {
                SettingsCardItem(position = GroupPosition.SINGLE) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text("Haptic Feedback", fontWeight = FontWeight.SemiBold) },
                        leadingContent = {
                            Icon(Icons.Default.Vibration, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        },
                        trailingContent = {
                            Switch(
                                checked = settings.hapticsEnabled,
                                onCheckedChange = { haptics ->
                                    viewModel.updateSettings { it.copy(hapticsEnabled = haptics) }
                                }
                            )
                        }
                    )
                }
            }

            // SECTION 5: FEATURES
            item {
                SectionHeader(
                    title = "Island Features",
                    icon = Icons.Default.Apps
                )
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SettingsCardItem(position = GroupPosition.TOP) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Media Player", fontWeight = FontWeight.SemiBold) },
                            leadingContent = {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color(0xFF64B5F6))
                            },
                            trailingContent = {
                                Switch(
                                    checked = settings.showMedia,
                                    onCheckedChange = { show ->
                                        viewModel.updateSettings { it.copy(showMedia = show) }
                                    }
                                )
                            }
                        )
                    }

                    SettingsCardItem(position = GroupPosition.MIDDLE) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Incoming Notifications", fontWeight = FontWeight.SemiBold) },
                            leadingContent = {
                                Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFFFFB74D))
                            },
                            trailingContent = {
                                Switch(
                                    checked = settings.showNotifications,
                                    onCheckedChange = { show ->
                                        viewModel.updateSettings { it.copy(showNotifications = show) }
                                    }
                                )
                            }
                        )
                    }

                    SettingsCardItem(position = GroupPosition.MIDDLE) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Live Progress & Activities", fontWeight = FontWeight.SemiBold) },
                            leadingContent = {
                                Icon(Icons.Default.Animation, contentDescription = null, tint = Color(0xFF42A5F5))
                            },
                            trailingContent = {
                                Switch(
                                    checked = settings.showLiveProgress,
                                    onCheckedChange = { show ->
                                        viewModel.updateSettings { it.copy(showLiveProgress = show) }
                                    }
                                )
                            }
                        )
                    }

                    if (settings.showNotifications || settings.showLiveProgress) {
                        SettingsCardItem(position = GroupPosition.MIDDLE) {
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                headlineContent = { Text("Per-App Notification Filter", fontWeight = FontWeight.SemiBold) },
                                leadingContent = {
                                    Icon(Icons.Default.Apps, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                trailingContent = {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.loadInstalledApps(context)
                                            showAppFilterSheet = true
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Filter Apps")
                                    }
                                }
                            )
                        }
                    }

                    SettingsCardItem(position = GroupPosition.BOTTOM) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Screen Recording Indicator", fontWeight = FontWeight.SemiBold) },
                            leadingContent = {
                                Icon(Icons.Default.Videocam, contentDescription = null, tint = Color(0xFFFF5252))
                            },
                            trailingContent = {
                                Switch(
                                    checked = settings.showScreenRecording,
                                    onCheckedChange = { show ->
                                        viewModel.updateSettings { it.copy(showScreenRecording = show) }
                                    }
                                )
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // App Filter Modal Bottom Sheet
    if (showAppFilterSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val coroutineScope = rememberCoroutineScope()

        ModalBottomSheet(
            onDismissRequest = { showAppFilterSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Per-App Notification Filter",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = appFilterQuery,
                    onValueChange = { appFilterQuery = it },
                    placeholder = { Text("Search installed apps...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                val filteredApps = remember(installedApps, appFilterQuery) {
                    if (appFilterQuery.isBlank()) installedApps
                    else installedApps.filter {
                        it.appName.contains(appFilterQuery, ignoreCase = true) ||
                                it.packageName.contains(appFilterQuery, ignoreCase = true)
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        ListItem(
                            headlineContent = { Text(app.appName, fontWeight = FontWeight.Medium) },
                            leadingContent = {
                                if (app.icon != null) {
                                    val bitmap = remember(app.icon) { app.icon.toBitmap(96, 96) }
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(36.dp))
                                }
                            },
                            trailingContent = {
                                Switch(
                                    checked = !app.isBlocked,
                                    onCheckedChange = {
                                        viewModel.toggleAppBlocked(app.packageName)
                                    }
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badge: String? = null,
    badgeColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (badge != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(badgeColor.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badge,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            }
        }
    }
}

enum class GroupPosition {
    TOP,
    MIDDLE,
    BOTTOM,
    SINGLE
}

fun getGroupedShape(position: GroupPosition): RoundedCornerShape {
    val outerRadius = 22.dp
    val innerRadius = 4.dp
    return when (position) {
        GroupPosition.TOP -> RoundedCornerShape(
            topStart = outerRadius,
            topEnd = outerRadius,
            bottomStart = innerRadius,
            bottomEnd = innerRadius
        )
        GroupPosition.MIDDLE -> RoundedCornerShape(innerRadius)
        GroupPosition.BOTTOM -> RoundedCornerShape(
            topStart = innerRadius,
            topEnd = innerRadius,
            bottomStart = outerRadius,
            bottomEnd = outerRadius
        )
        GroupPosition.SINGLE -> RoundedCornerShape(outerRadius)
    }
}

@Composable
fun SettingsCardItem(
    position: GroupPosition,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = getGroupedShape(position),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        content()
    }
}

@Composable
fun SliderSettingItem(
    label: String,
    value: Float,
    unit: String,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${if (unit == "x" || unit == "dp") String.format(Locale.US, "%.1f", value).replace(".0", "") else value.toInt()} $unit",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.height(24.dp)
        )
    }
}
