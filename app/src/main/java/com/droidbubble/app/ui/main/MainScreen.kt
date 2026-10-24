package com.droidbubble.app.ui.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
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
import com.droidbubble.app.service.ScreenRecordingService
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val PRESET_COLORS = listOf(
    0xFF00E5FF to "Cyan",
    0xFFFF2A85 to "Neon Pink",
    0xFF00E676 to "Emerald",
    0xFF7C4DFF to "Purple",
    0xFFFFAB00 to "Amber",
    0xFFFFFFFF to "Clean White",
    0xFFFF1744 to "Crimson",
    0xFF64B5F6 to "Sky Blue"
)

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

    var showColorPicker by remember { mutableStateOf(false) }
    var showAppFilterSheet by remember { mutableStateOf(false) }
    var appFilterQuery by remember { mutableStateOf("") }

    // Screen Recording Permission Launcher
    val screenRecordingLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val recordIntent = Intent(context, ScreenRecordingService::class.java).apply {
                action = ScreenRecordingService.ACTION_START
                putExtra(ScreenRecordingService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenRecordingService.EXTRA_RESULT_DATA, result.data)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(recordIntent)
            } else {
                context.startService(recordIntent)
            }
        }
    }

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

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Droidbubble",
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp
                        )
                        Text(
                            text = "Dynamic Island for Android",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Preview Card
            item {
                IslandPreviewCard(settings = settings)
            }

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

                items(pendingPermissions, key = { it.id }) { perm ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                        )
                    ) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = {
                                Text(perm.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            },
                            supportingContent = {
                                Text(perm.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

            // SECTION 2: ISLAND CUSTOMISATION
            item {
                SectionHeader(
                    title = "Island Customisation",
                    icon = Icons.Default.Tune
                )
            }

            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Enable Switch
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Enable Island", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("Show the floating overlay across all apps") },
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

                        Spacer(modifier = Modifier.height(8.dp))

                        // Width Slider
                        SliderSettingItem(
                            label = "Width",
                            value = settings.widthDp.toFloat(),
                            unit = "dp",
                            valueRange = 120f..260f,
                            onValueChange = { viewModel.updateSettings { s -> s.copy(widthDp = it.roundToInt()) } }
                        )

                        // Height Slider
                        SliderSettingItem(
                            label = "Height",
                            value = settings.heightDp.toFloat(),
                            unit = "dp",
                            valueRange = 26f..56f,
                            onValueChange = { viewModel.updateSettings { s -> s.copy(heightDp = it.roundToInt()) } }
                        )

                        // Corner Radius Slider
                        SliderSettingItem(
                            label = "Corner Radius",
                            value = settings.cornerRadiusDp.toFloat(),
                            unit = "dp",
                            valueRange = 10f..28f,
                            onValueChange = { viewModel.updateSettings { s -> s.copy(cornerRadiusDp = it.roundToInt()) } }
                        )

                        // Top Offset (Camera Cutout Alignment)
                        SliderSettingItem(
                            label = "Top Offset (Notch Line-up)",
                            value = settings.topOffsetDp.toFloat(),
                            unit = "dp",
                            valueRange = 0f..60f,
                            onValueChange = { viewModel.updateSettings { s -> s.copy(topOffsetDp = it.roundToInt()) } }
                        )

                        // Horizontal Offset
                        SliderSettingItem(
                            label = "Horizontal Offset",
                            value = settings.horizontalOffsetDp.toFloat(),
                            unit = "dp",
                            valueRange = -50f..50f,
                            onValueChange = { viewModel.updateSettings { s -> s.copy(horizontalOffsetDp = it.roundToInt()) } }
                        )

                        // Expanded Scale Slider
                        SliderSettingItem(
                            label = "Expanded Card Scale",
                            value = settings.expandedScale,
                            unit = "x",
                            valueRange = 0.8f..1.3f,
                            steps = 5,
                            onValueChange = { viewModel.updateSettings { s -> s.copy(expandedScale = (it * 10).roundToInt() / 10f) } }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Outline Toggle
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Island Outline", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("Display a luminous border around the island") },
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

                        AnimatedVisibility(visible = settings.showOutline) {
                            Column {
                                // Outline Thickness
                                SliderSettingItem(
                                    label = "Outline Thickness",
                                    value = settings.outlineThicknessDp,
                                    unit = "dp",
                                    valueRange = 0.5f..4.0f,
                                    onValueChange = { viewModel.updateSettings { s -> s.copy(outlineThicknessDp = (it * 10).roundToInt() / 10f) } }
                                )

                                // Outline Colour Picker Button
                                ListItem(
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    headlineContent = { Text("Outline Colour", fontWeight = FontWeight.SemiBold) },
                                    supportingContent = { Text("Tap to select vibrant border accent") },
                                    leadingContent = {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color(settings.outlineColorArgb))
                                                .border(1.dp, Color.Gray, CircleShape)
                                        )
                                    },
                                    trailingContent = {
                                        OutlinedButton(
                                            onClick = { showColorPicker = true },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Change")
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Haptics Toggle
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Haptic Feedback", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("Tactile tick on expand and collapse") },
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
            }

            // SECTION 3: WHAT TO SHOW
            item {
                SectionHeader(
                    title = "Island Features",
                    icon = Icons.Default.Apps
                )
            }

            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Media Feature
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Media Player", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("Equalizer, album art, progress bar, playback controls") },
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

                        // Notifications Feature
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Incoming Notifications", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("Pop-in dynamic bubble with quick actions & replies") },
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

                        // Per-app Filter Button
                        AnimatedVisibility(visible = settings.showNotifications) {
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                headlineContent = { Text("Per-App Notification Filter", fontWeight = FontWeight.SemiBold) },
                                supportingContent = { Text("${settings.blockedPackages.size} apps muted") },
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

                        // Screen Recording Feature
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Screen Recording Indicator", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("Live timer, pulsing red dot and stop/pause controls") },
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

                        // Trigger Screen Recording Button
                        Spacer(modifier = Modifier.height(8.dp))
                        FilledTonalButton(
                            onClick = {
                                val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                                screenRecordingLauncher.launch(projectionManager.createScreenCaptureIntent())
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Test Screen Recording")
                        }
                    }
                }
            }

            // SECTION 4: ABOUT
            item {
                SectionHeader(
                    title = "About",
                    icon = Icons.Default.Info
                )
            }

            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black)
                                    .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp, 10.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = "Droidbubble",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Version 1.0 • Material 3 Expressive",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "A native Android adaptation of Dynamic Island powered by Jetpack Compose. Seamlessly merges active media playback, real-time screen recording, and pop-in notification alerts right around your device camera cutout.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Color Picker Dialog
    if (showColorPicker) {
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text("Choose Outline Colour", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Select a preset or custom accent:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        PRESET_COLORS.take(4).forEach { (colorArgb, name) ->
                            ColorCircleItem(
                                colorArgb = colorArgb,
                                name = name,
                                isSelected = settings.outlineColorArgb == colorArgb,
                                onClick = {
                                    viewModel.updateSettings { it.copy(outlineColorArgb = colorArgb) }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        PRESET_COLORS.drop(4).forEach { (colorArgb, name) ->
                            ColorCircleItem(
                                colorArgb = colorArgb,
                                name = name,
                                isSelected = settings.outlineColorArgb == colorArgb,
                                onClick = {
                                    viewModel.updateSettings { it.copy(outlineColorArgb = colorArgb) }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showColorPicker = false }) {
                    Text("Done")
                }
            }
        )
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
                Text(
                    text = "Toggle off apps to ignore their notifications on the island",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            supportingContent = { Text(app.packageName, style = MaterialTheme.typography.bodySmall, maxLines = 1) },
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
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                text = "${if (unit == "x" || unit == "dp") String.format("%.1f", value).replace(".0", "") else value.toInt()} $unit",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.height(20.dp)
        )
    }
}

@Composable
fun IslandPreviewCard(settings: IslandSettings) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Live Island Preview",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Simulated phone notch area with island
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF18181B)),
                contentAlignment = Alignment.TopCenter
            ) {
                // The Island Pill Preview
                Box(
                    modifier = Modifier
                        .padding(top = (settings.topOffsetDp / 2).coerceAtLeast(6).dp)
                        .width((settings.widthDp * 0.85f).dp)
                        .height((settings.heightDp * 0.85f).dp)
                        .clip(RoundedCornerShape((settings.cornerRadiusDp * 0.85f).dp))
                        .background(Color.Black)
                        .then(
                            if (settings.showOutline) {
                                Modifier.border(
                                    width = settings.outlineThicknessDp.dp,
                                    color = Color(settings.outlineColorArgb),
                                    shape = RoundedCornerShape((settings.cornerRadiusDp * 0.85f).dp)
                                )
                            } else Modifier
                        )
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF00E5FF))
                        )
                        Text(
                            text = "Droidbubble",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF5252))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ColorCircleItem(
    colorArgb: Long,
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(colorArgb))
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (colorArgb == 0xFFFFFFFFL) Color.Black else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(name, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}
