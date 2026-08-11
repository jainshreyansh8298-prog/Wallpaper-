package com.example.celestial

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.cos

enum class TimeOfDayType(
    val label: String,
    val subtitle: String,
    val iconSymbol: String
) {
    SUNRISE("Sunrise", "Golden Horizon • Morning Glow", "🌅"),
    NOON("Solar Noon", "Peak Daylight • Radiant Energy", "☀️"),
    AFTERNOON("Late Afternoon", "Sunset Amber • Dusky Rays", "🌇"),
    NIGHT("Lunar Night", "Starry Sky • Moon Phase Active", "🌙")
}

enum class MoonPhaseType(
    val phaseName: String,
    val symbol: String,
    val description: String
) {
    NEW_MOON("New Moon", "🌑", "Dark celestial disc"),
    WAXING_CRESCENT("Waxing Crescent", "🌒", "Growing right crescent"),
    FIRST_QUARTER("First Quarter", "🌓", "Half moon lit on right"),
    WAXING_GIBBOUS("Waxing Gibbous", "🌔", "Bulging right moon"),
    FULL_MOON("Full Moon", "🌕", "Fully illuminated orb"),
    WANING_GIBBOUS("Waning Gibbous", "🌖", "Diminishing left moon"),
    LAST_QUARTER("Last Quarter", "🌗", "Half moon lit on left"),
    WANING_CRESCENT("Waning Crescent", "🌘", "Slender left crescent")
}

data class CelestialInfo(
    val timeOfDay: TimeOfDayType,
    val hourOfDay: Int,
    val moonPhase: MoonPhaseType,
    val phaseRatio: Float, // 0.0 to 1.0
    val illuminationPercent: Int, // 0 to 100%
    val ageInDays: Float
)

object CelestialManager {
    // Epoch reference: Known New Moon on Jan 11, 2024 at 11:57 UTC
    private const val NEW_MOON_EPOCH_MS = 1704974220000L
    private const val SYNODIC_MONTH_MS = 2551442878.0 // 29.5305877 days in ms

    fun getCelestialInfo(nowMs: Long = System.currentTimeMillis()): CelestialInfo {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.timeInMillis = nowMs
        val hour = cal.get(Calendar.HOUR_OF_DAY)

        val timeOfDay = when (hour) {
            in 6..8 -> TimeOfDayType.SUNRISE
            in 9..14 -> TimeOfDayType.NOON
            in 15..17 -> TimeOfDayType.AFTERNOON
            else -> TimeOfDayType.NIGHT
        }

        // Astronomical moon phase calculation
        val diffMs = (nowMs - NEW_MOON_EPOCH_MS).toDouble()
        var phaseRatio = (diffMs % SYNODIC_MONTH_MS) / SYNODIC_MONTH_MS
        if (phaseRatio < 0) phaseRatio += 1.0

        val ageInDays = (phaseRatio * 29.5305877).toFloat()
        val illuminationPercent = (((1.0 - cos(phaseRatio * 2.0 * PI)) / 2.0) * 100.0).toInt().coerceIn(0, 100)

        val moonPhase = when {
            phaseRatio < 0.06 || phaseRatio >= 0.94 -> MoonPhaseType.NEW_MOON
            phaseRatio < 0.19 -> MoonPhaseType.WAXING_CRESCENT
            phaseRatio < 0.31 -> MoonPhaseType.FIRST_QUARTER
            phaseRatio < 0.44 -> MoonPhaseType.WAXING_GIBBOUS
            phaseRatio < 0.56 -> MoonPhaseType.FULL_MOON
            phaseRatio < 0.69 -> MoonPhaseType.WANING_GIBBOUS
            phaseRatio < 0.81 -> MoonPhaseType.LAST_QUARTER
            else -> MoonPhaseType.WANING_CRESCENT
        }

        return CelestialInfo(
            timeOfDay = timeOfDay,
            hourOfDay = hour,
            moonPhase = moonPhase,
            phaseRatio = phaseRatio.toFloat(),
            illuminationPercent = illuminationPercent,
            ageInDays = ageInDays
        )
    }
}
