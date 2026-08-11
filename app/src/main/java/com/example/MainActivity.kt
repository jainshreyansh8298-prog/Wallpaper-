package com.example

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCardBg
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberDarkBg
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.wallpaper.ColorPreset
import com.example.wallpaper.EffectMode
import com.example.wallpaper.WallpaperConfig
import com.example.wallpaper.WallpaperRenderer
import com.example.wallpaper.WallpaperSettingsStore
import kotlinx.coroutines.delay

private val NothingRed = Color(0xFFFF0037)
private val NothingDark = Color(0xFF000000)
private val NothingCardBg = Color(0xFF0C0C0C)
private val NothingCardBorder = Color(0xFF222222)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = NothingDark
                ) { padding ->
                    LiveWallpaperAppScreen(
                        modifier = Modifier.padding(padding),
                        onApplyWallpaper = { launchWallpaperPicker() }
                    )
                }
            }
        }
    }

    private fun launchWallpaperPicker() {
        try {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this@MainActivity, ProceduralWallpaperService::class.java)
                )
            }
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
                startActivity(intent)
            } catch (ex: Exception) {
                Toast.makeText(this, "Opening Wallpaper Settings...", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LiveWallpaperAppScreen(
    modifier: Modifier = Modifier,
    onApplyWallpaper: () -> Unit
) {
    val context = LocalContext.current
    var config by remember { mutableStateOf(WallpaperSettingsStore.loadConfig(context)) }

    val updateConfig = { newConfig: WallpaperConfig ->
        config = newConfig
        WallpaperSettingsStore.saveConfig(context, newConfig)
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Nothing OS Style Header ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "● NOTHING // SIMULATOR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NothingRed,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Physics & Matrix OS",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF161616))
                    .border(1.dp, NothingRed.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(NothingRed)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "APK < 0.4 MB",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // --- Interactive Preview Card ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.5.dp, NothingCardBorder, RoundedCornerShape(16.dp))
                .testTag("wallpaper_preview_card"),
            colors = CardDefaults.cardColors(containerColor = NothingCardBg)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                LiveWallpaperInteractiveCanvas(
                    config = config,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = "Interactive Touch",
                            tint = NothingRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Touch & Slide Interactive Canvas",
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // --- Apply Live Wallpaper Button (Lock Screen & Home Screen) ---
        Button(
            onClick = onApplyWallpaper,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("apply_wallpaper_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = NothingRed,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Wallpaper,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "SET AS LOCK & HOME WALLPAPER",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 0.8.sp
                )
            }
        }

        // --- Unlock Tracker Telemetry Panel ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NothingCardBg),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NothingRed.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = NothingRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "UNLOCK BEAD CONTAINER STATS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }

                    Button(
                        onClick = {
                            WallpaperSettingsStore.resetUnlockStats(context)
                            config = WallpaperSettingsStore.loadConfig(context)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF222222),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(text = "RESET", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${config.unlockCount}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NothingRed
                        )
                        Text(text = "Unlocks & Beads", fontSize = 11.sp, color = Color.Gray)
                    }

                    val elapsedSec = ((System.currentTimeMillis() - config.trackedTimeStartMs) / 1000L).coerceAtLeast(0L)
                    val hrs = elapsedSec / 3600
                    val mins = (elapsedSec % 3600) / 60

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${hrs}h ${mins}m",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(text = "Time Tracked", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }

        // --- Color Preset Selector ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NothingCardBg),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NothingCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = NothingRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "COLOR PALETTES",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ColorPreset.values().forEach { preset ->
                        val isSelected = config.colorPreset == preset
                        val borderColor by animateColorAsState(
                            if (isSelected) NothingRed else Color.Transparent,
                            label = "border"
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(preset.bgSecondary))
                                .border(2.dp, borderColor, RoundedCornerShape(12.dp))
                                .clickable { updateConfig(config.copy(colorPreset = preset)) }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("preset_${preset.name.lowercase()}")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(preset.accentPrimary))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(preset.accentSecondary))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(preset.accentTertiary))
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = preset.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) NothingRed else Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Effect Mode Selector ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NothingCardBg),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NothingCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = null,
                        tint = NothingRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EFFECT & MATRIX MODES",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EffectMode.values().forEach { mode ->
                        val isSelected = config.effectMode == mode
                        val cardBg = if (isSelected) Color(0xFF1E060A) else Color(0xFF121212)
                        val border = if (isSelected) NothingRed else Color(0xFF222222)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(cardBg)
                                .border(1.5.dp, border, RoundedCornerShape(12.dp))
                                .clickable { updateConfig(config.copy(effectMode = mode)) }
                                .padding(12.dp)
                                .testTag("effect_${mode.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mode.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) NothingRed else Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = mode.description,
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = NothingRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Controls & Toggles ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NothingCardBg),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NothingCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = NothingRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PHYSICS & INTERACTION CONTROLS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Drag Painting Mode Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Touch Painting Mode",
                            fontSize = 13.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Paints persistent glowing particle trails on drag",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = config.paintingMode,
                        onCheckedChange = { updateConfig(config.copy(paintingMode = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NothingRed,
                            checkedTrackColor = Color(0xFF4A0010)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Zero-Gravity Mode Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Zero-Gravity Mode",
                            fontSize = 13.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Weightless particle drift without friction or wall bounces",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = config.zeroGravityMode,
                        onCheckedChange = { updateConfig(config.copy(zeroGravityMode = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NothingRed,
                            checkedTrackColor = Color(0xFF4A0010)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tactile Haptics Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tactile Touch Haptics",
                            fontSize = 13.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Vibration ticks on touch ripple impact",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = config.enableHaptics,
                        onCheckedChange = { updateConfig(config.copy(enableHaptics = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NothingRed,
                            checkedTrackColor = Color(0xFF4A0010)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LiveWallpaperInteractiveCanvas(
    config: WallpaperConfig,
    modifier: Modifier = Modifier
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val renderer = remember {
        WallpaperRenderer().apply {
            onHapticFeedbackNeeded = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
            }
        }
    }

    // Sync config changes into preview renderer
    LaunchedEffect(config) {
        renderer.config = config
    }

    var frameTime by remember { mutableStateOf(0L) }

    // Animation Loop for Live Preview
    LaunchedEffect(config.targetFps) {
        var lastNano = System.nanoTime()
        val frameTargetMs = (1000L / config.targetFps.coerceIn(15, 60)).coerceAtLeast(1L)

        while (true) {
            val nowNano = System.nanoTime()
            val deltaSec = ((nowNano - lastNano) / 1_000_000_000.0f).coerceIn(0.001f, 0.1f)
            lastNano = nowNano

            renderer.update(deltaSec)
            frameTime = nowNano

            delay(frameTargetMs)
        }
    }

    Canvas(
        modifier = modifier.pointerInteropFilter { motionEvent ->
            renderer.handleMotionEvent(motionEvent)
            true
        }
    ) {
        // Trigger recomposition on frameTime tick
        @Suppress("UNUSED_EXPRESSION")
        frameTime

        renderer.setDimensions(size.width.toInt(), size.height.toInt())
        renderer.render(drawContext.canvas.nativeCanvas, config.targetFps)
    }
}
