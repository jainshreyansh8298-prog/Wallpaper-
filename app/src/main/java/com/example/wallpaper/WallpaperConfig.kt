package com.example.wallpaper

import android.content.Context
import android.content.SharedPreferences

enum class ColorPreset(
    val title: String,
    val bgPrimary: Int,
    val bgSecondary: Int,
    val accentPrimary: Int,
    val accentSecondary: Int,
    val accentTertiary: Int
) {
    NOTHING_MONO(
        title = "Nothing Monochrome",
        bgPrimary = 0xFF000000.toInt(),
        bgSecondary = 0xFF0B0B0B.toInt(),
        accentPrimary = 0xFFFFFFFF.toInt(), // Glyph White
        accentSecondary = 0xFFFF0037.toInt(), // Signature Nothing Red
        accentTertiary = 0xFF777777.toInt()  // Dot Matrix Gray
    ),
    CYBERPUNK(
        title = "Cyberpunk Neon",
        bgPrimary = 0xFF0A0E17.toInt(),
        bgSecondary = 0xFF121B2D.toInt(),
        accentPrimary = 0xFF00F0FF.toInt(), // Cyan
        accentSecondary = 0xFFFF007A.toInt(), // Neon Magenta
        accentTertiary = 0xFF7000FF.toInt()  // Deep Purple
    ),
    AURORA(
        title = "Aurora Borealis",
        bgPrimary = 0xFF050B14.toInt(),
        bgSecondary = 0xFF0A192F.toInt(),
        accentPrimary = 0xFF00FF87.toInt(), // Aurora Emerald
        accentSecondary = 0xFF60EFFF.toInt(), // Ice Blue
        accentTertiary = 0xFFA855F7.toInt()  // Glowing Violet
    ),
    COSMIC(
        title = "Cosmic Nebula",
        bgPrimary = 0xFF030712.toInt(),
        bgSecondary = 0xFF0F172A.toInt(),
        accentPrimary = 0xFF3B82F6.toInt(), // Blue
        accentSecondary = 0xFFF59E0B.toInt(), // Gold
        accentTertiary = 0xFFEF4444.toInt()  // Crimson
    ),
    LAVA(
        title = "Lava Fluid",
        bgPrimary = 0xFF0F0505.toInt(),
        bgSecondary = 0xFF1C0A0A.toInt(),
        accentPrimary = 0xFFF97316.toInt(), // Amber Orange
        accentSecondary = 0xFFDC2626.toInt(), // Lava Red
        accentTertiary = 0xFFFACC15.toInt()  // Gold Glow
    ),
    SOLAR(
        title = "Solar Matrix",
        bgPrimary = 0xFF080E1E.toInt(),
        bgSecondary = 0xFF101D38.toInt(),
        accentPrimary = 0xFFFFD700.toInt(), // Gold
        accentSecondary = 0xFF00E5FF.toInt(), // Bright Cyan
        accentTertiary = 0xFFFF2A6D.toInt()  // Neon Pink
    )
}

enum class EffectMode(val title: String, val description: String) {
    NOTHING_MATRIX("Nothing Matrix", "Reactive LED dot-matrix grid with glyph waves and red pulses"),
    WATER_BEAD_CONTAINER("Water Bead Container", "Fluid container where each unlock adds a floating glowing water bead"),
    PAINTING_CANVAS("Painting Canvas", "Interactive glowing stroke canvas with touch-drag synthesis"),
    QUANTUM_GRID("Quantum Grid", "Distortable holographic grid mesh with reactive shockwaves"),
    NEON_PARTICLES("Neon Particles", "Floating particle constellation with multi-touch gravitational field"),
    AURORA_FLOW("Aurora Flow", "Procedural wave ribbons with interactive vortex swirls"),
    COSMIC_PLASMA("Cosmic Plasma", "Mathematical plasma fields with twinkling star clusters")
}

data class WallpaperConfig(
    val colorPreset: ColorPreset = ColorPreset.NOTHING_MONO,
    val effectMode: EffectMode = EffectMode.NOTHING_MATRIX,
    val particleCount: Int = 120,
    val speedScale: Float = 1.0f,
    val rippleIntensity: Float = 1.0f,
    val touchSensitivity: Float = 1.0f,
    val targetFps: Int = 60,
    val showTouchTrails: Boolean = true,
    val enableWebConnections: Boolean = true,
    val zeroGravityMode: Boolean = false,
    val enableHaptics: Boolean = true,
    val paintingMode: Boolean = false,
    val unlockCount: Int = 0,
    val trackedTimeStartMs: Long = System.currentTimeMillis()
)

object WallpaperSettingsStore {
    private const val PREFS_NAME = "procedural_wallpaper_prefs"

    private const val KEY_COLOR_PRESET = "color_preset"
    private const val KEY_EFFECT_MODE = "effect_mode"
    private const val KEY_PARTICLE_COUNT = "particle_count"
    private const val KEY_SPEED_SCALE = "speed_scale"
    private const val KEY_RIPPLE_INTENSITY = "ripple_intensity"
    private const val KEY_TOUCH_SENSITIVITY = "touch_sensitivity"
    private const val KEY_TARGET_FPS = "target_fps"
    private const val KEY_TOUCH_TRAILS = "touch_trails"
    private const val KEY_WEB_CONNECTIONS = "web_connections"
    private const val KEY_ZERO_GRAVITY = "zero_gravity"
    private const val KEY_ENABLE_HAPTICS = "enable_haptics"
    private const val KEY_PAINTING_MODE = "painting_mode"
    private const val KEY_UNLOCK_COUNT = "unlock_count"
    private const val KEY_TRACKED_TIME_START = "tracked_time_start"

    fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun loadConfig(context: Context): WallpaperConfig {
        val prefs = getPrefs(context)
        val presetOrdinal = prefs.getInt(KEY_COLOR_PRESET, ColorPreset.NOTHING_MONO.ordinal)
        val effectOrdinal = prefs.getInt(KEY_EFFECT_MODE, EffectMode.NOTHING_MATRIX.ordinal)

        val preset = ColorPreset.values().getOrElse(presetOrdinal) { ColorPreset.NOTHING_MONO }
        val effect = EffectMode.values().getOrElse(effectOrdinal) { EffectMode.NOTHING_MATRIX }

        var startMs = prefs.getLong(KEY_TRACKED_TIME_START, 0L)
        if (startMs <= 0L) {
            startMs = System.currentTimeMillis()
            prefs.edit().putLong(KEY_TRACKED_TIME_START, startMs).apply()
        }

        return WallpaperConfig(
            colorPreset = preset,
            effectMode = effect,
            particleCount = prefs.getInt(KEY_PARTICLE_COUNT, 120),
            speedScale = prefs.getFloat(KEY_SPEED_SCALE, 1.0f),
            rippleIntensity = prefs.getFloat(KEY_RIPPLE_INTENSITY, 1.0f),
            touchSensitivity = prefs.getFloat(KEY_TOUCH_SENSITIVITY, 1.0f),
            targetFps = prefs.getInt(KEY_TARGET_FPS, 60),
            showTouchTrails = prefs.getBoolean(KEY_TOUCH_TRAILS, true),
            enableWebConnections = prefs.getBoolean(KEY_WEB_CONNECTIONS, true),
            zeroGravityMode = prefs.getBoolean(KEY_ZERO_GRAVITY, false),
            enableHaptics = prefs.getBoolean(KEY_ENABLE_HAPTICS, true),
            paintingMode = prefs.getBoolean(KEY_PAINTING_MODE, false),
            unlockCount = prefs.getInt(KEY_UNLOCK_COUNT, 1),
            trackedTimeStartMs = startMs
        )
    }

    fun saveConfig(context: Context, config: WallpaperConfig) {
        getPrefs(context).edit()
            .putInt(KEY_COLOR_PRESET, config.colorPreset.ordinal)
            .putInt(KEY_EFFECT_MODE, config.effectMode.ordinal)
            .putInt(KEY_PARTICLE_COUNT, config.particleCount)
            .putFloat(KEY_SPEED_SCALE, config.speedScale)
            .putFloat(KEY_RIPPLE_INTENSITY, config.rippleIntensity)
            .putFloat(KEY_TOUCH_SENSITIVITY, config.touchSensitivity)
            .putInt(KEY_TARGET_FPS, config.targetFps)
            .putBoolean(KEY_TOUCH_TRAILS, config.showTouchTrails)
            .putBoolean(KEY_WEB_CONNECTIONS, config.enableWebConnections)
            .putBoolean(KEY_ZERO_GRAVITY, config.zeroGravityMode)
            .putBoolean(KEY_ENABLE_HAPTICS, config.enableHaptics)
            .putBoolean(KEY_PAINTING_MODE, config.paintingMode)
            .putInt(KEY_UNLOCK_COUNT, config.unlockCount)
            .putLong(KEY_TRACKED_TIME_START, config.trackedTimeStartMs)
            .apply()
    }

    fun incrementUnlockCount(context: Context): Int {
        val prefs = getPrefs(context)
        val current = prefs.getInt(KEY_UNLOCK_COUNT, 0)
        val updated = current + 1
        prefs.edit().putInt(KEY_UNLOCK_COUNT, updated).apply()
        return updated
    }

    fun resetUnlockStats(context: Context) {
        getPrefs(context).edit()
            .putInt(KEY_UNLOCK_COUNT, 0)
            .putLong(KEY_TRACKED_TIME_START, System.currentTimeMillis())
            .apply()
    }
}
