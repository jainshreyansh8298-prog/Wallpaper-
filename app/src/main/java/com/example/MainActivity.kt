package com.example

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Canvas
import android.os.Bundle
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.celestial.CelestialInfo
import com.example.celestial.CelestialManager
import com.example.ui.theme.MyApplicationTheme
import com.example.wallpaper.ColorPreset
import com.example.wallpaper.EffectMode
import com.example.wallpaper.WallpaperConfig
import com.example.wallpaper.WallpaperRenderer
import com.example.wallpaper.WallpaperSettingsStore
import com.example.wellbeing.DigitalWellbeingManager

private val NothingRed = Color(0xFFFF0037)
private val NothingDark = Color(0xFF060606)
private val NothingCardBg = Color(0xFF121212)
private val NothingCardBorder = Color(0xFF242424)
private val NothingAccentGold = Color(0xFFFFB703)

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

@Composable
fun LiveWallpaperAppScreen(
    modifier: Modifier = Modifier,
    onApplyWallpaper: () -> Unit
) {
    val context = LocalContext.current
    var config by remember { mutableStateOf(WallpaperSettingsStore.loadConfig(context)) }
    val celestialInfo = remember { CelestialManager.getCelestialInfo() }
    val wellbeingStats = remember(config.unlockCount) { DigitalWellbeingManager.getStats(context) }

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
        // --- Header Section: Celestial Moon Phase & Time Widget ---
        CelestialHeaderCard(celestialInfo = celestialInfo)

        // --- Pinterest Hero Card: Native Interactive Canvas Preview ---
        PinterestCanvasPreviewCard(
            config = config,
            onModeSelect = { mode -> updateConfig(config.copy(effectMode = mode)) }
        )

        // --- Apply Live Wallpaper Action Button ---
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
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Wallpaper,
                    contentDescription = "Set Wallpaper",
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "SET LOCK & HOME WALLPAPER",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        // --- Digital Wellbeing Dashboard Card ---
        WellbeingDashboardCard(
            stats = wellbeingStats,
            onResetStats = {
                WallpaperSettingsStore.resetUnlockStats(context)
                config = WallpaperSettingsStore.loadConfig(context)
            }
        )

        // --- Bento Grid 1: Physics, Drag Paint & Haptics ---
        BentoPhysicsCard(
            config = config,
            updateConfig = updateConfig
        )

        // --- Bento Grid 2: Shader & Matrix Effect Modes ---
        BentoShaderModesCard(
            config = config,
            updateConfig = updateConfig
        )

        // --- Bento Grid 3: Color Palettes ---
        BentoColorPalettesCard(
            config = config,
            updateConfig = updateConfig
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun CelestialHeaderCard(celestialInfo: CelestialInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NothingCardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, NothingRed.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(NothingRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CELESTIAL TIME • ${celestialInfo.timeOfDay.label.uppercase()}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NothingRed,
                        letterSpacing = 1.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "${celestialInfo.moonPhase.symbol} ${celestialInfo.moonPhase.phaseName}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${celestialInfo.illuminationPercent}% Illuminated • Day ${"%.1f".format(celestialInfo.ageInDays)} of 29.5",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E1E1E))
                    .border(1.dp, NothingAccentGold.copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = celestialInfo.moonPhase.symbol,
                    fontSize = 28.sp
                )
            }
        }
    }
}

@Composable
fun PinterestCanvasPreviewCard(
    config: WallpaperConfig,
    onModeSelect: (EffectMode) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(290.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.5.dp, NothingCardBorder, RoundedCornerShape(20.dp))
            .testTag("wallpaper_preview_card"),
        colors = CardDefaults.cardColors(containerColor = NothingCardBg)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            NativeWallpaperPreviewCanvas(
                config = config,
                modifier = Modifier.fillMaxSize()
            )

            // Top Overlay Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .border(1.dp, NothingRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
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
                            text = "Touch & Slide Canvas",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(NothingRed)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = config.effectMode.title.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            // Bottom Mode Quick Switch Bar
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(12.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EffectMode.values().forEach { mode ->
                    val isSelected = config.effectMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) NothingRed else Color.Black.copy(alpha = 0.8f))
                            .clickable { onModeSelect(mode) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = mode.title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WellbeingDashboardCard(
    stats: com.example.wellbeing.WellbeingStats,
    onResetStats: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NothingCardBg),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NothingCardBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = NothingRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DIGITAL WELLBEING & WATER BEADS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.2.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF222222))
                        .clickable { onResetStats() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(text = "RESET", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stats.screenOnTimeFormatted,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = stats.focusStatus,
                        fontSize = 12.sp,
                        color = NothingRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${stats.totalUnlocks}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NothingRed
                    )
                    Text(
                        text = "Unlock Beads in Water",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Fluid Container Water Level Visual Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1E1E1E))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(stats.fluidContainerPercent)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(NothingRed)
                )
            }
        }
    }
}

@Composable
fun BentoPhysicsCard(
    config: WallpaperConfig,
    updateConfig: (WallpaperConfig) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NothingCardBg),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NothingCardBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = NothingRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PHYSICS & DRAG PAINT CONTROLS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 1.2.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

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
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Paints glowing neon strokes when dragging across canvas",
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

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Zero-Gravity Floating Mode",
                        fontSize = 13.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Particles float in weightless orbital paths",
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

            Spacer(modifier = Modifier.height(10.dp))

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
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Vibration pulses on touch ripple collisions",
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
}

@Composable
fun BentoShaderModesCard(
    config: WallpaperConfig,
    updateConfig: (WallpaperConfig) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NothingCardBg),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NothingCardBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ElectricBolt,
                    contentDescription = null,
                    tint = NothingRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PROCEDURAL SHADER & MATRIX MODES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 1.2.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EffectMode.values().forEach { mode ->
                    val isSelected = config.effectMode == mode
                    val cardBg = if (isSelected) Color(0xFF1E060A) else Color(0xFF161616)
                    val border = if (isSelected) NothingRed else Color(0xFF262626)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(cardBg)
                            .border(1.dp, border, RoundedCornerShape(12.dp))
                            .clickable { updateConfig(config.copy(effectMode = mode)) }
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mode.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
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
}

@Composable
fun BentoColorPalettesCard(
    config: WallpaperConfig,
    updateConfig: (WallpaperConfig) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NothingCardBg),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NothingCardBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
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
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 1.2.sp
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
                            .width(105.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF161616))
                            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
                            .clickable { updateConfig(config.copy(colorPreset = preset)) }
                            .padding(10.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .background(Color(preset.accentPrimary))
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .background(Color(preset.accentSecondary))
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
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
}

@Composable
fun NativeWallpaperPreviewCanvas(
    config: WallpaperConfig,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    AndroidView(
        factory = { ctx ->
            object : View(ctx) {
                val renderer = WallpaperRenderer().apply {
                    this.config = config
                    this.onHapticFeedbackNeeded = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }

                private var isRunning = true
                private val frameCallback = object : Choreographer.FrameCallback {
                    private var lastNano = System.nanoTime()

                    override fun doFrame(frameTimeNanos: Long) {
                        if (!isRunning) return
                        val deltaSec = ((frameTimeNanos - lastNano) / 1_000_000_000.0f).coerceIn(0.001f, 0.1f)
                        lastNano = frameTimeNanos

                        renderer.update(deltaSec)
                        postInvalidateOnAnimation()

                        if (isRunning) {
                            Choreographer.getInstance().postFrameCallback(this)
                        }
                    }
                }

                init {
                    Choreographer.getInstance().postFrameCallback(frameCallback)
                }

                override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
                    super.onSizeChanged(w, h, oldw, oldh)
                    renderer.setDimensions(w, h)
                }

                override fun onTouchEvent(event: MotionEvent): Boolean {
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                            parent?.requestDisallowInterceptTouchEvent(true)
                        }
                    }
                    renderer.handleMotionEvent(event)
                    postInvalidateOnAnimation()
                    return true
                }

                override fun onDraw(canvas: Canvas) {
                    super.onDraw(canvas)
                    renderer.render(canvas, config.targetFps)
                }

                override fun onDetachedFromWindow() {
                    super.onDetachedFromWindow()
                    isRunning = false
                }
            }
        },
        update = { view ->
            // Pass updated configuration into live view renderer
            (view as? View)?.let { v ->
                val field = v.javaClass.getDeclaredField("renderer").apply { isAccessible = true }
                val renderer = field.get(v) as? WallpaperRenderer
                renderer?.config = config
            }
        },
        modifier = modifier
    )
}
