package com.example.wellbeing

import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.SystemClock
import com.example.wallpaper.WallpaperSettingsStore
import java.util.Calendar

data class WellbeingStats(
    val screenOnTimeFormatted: String,
    val screenOnMinutes: Long,
    val totalUnlocks: Int,
    val focusStatus: String,
    val fluidContainerPercent: Float // 0.0f to 1.0f water level in container
)

object DigitalWellbeingManager {

    fun getStats(context: Context): WellbeingStats {
        val config = WallpaperSettingsStore.loadConfig(context)
        val unlocks = config.unlockCount.coerceAtLeast(1)

        // Calculate Screen Time based on elapsed uptime session
        val trackedStart = config.trackedTimeStartMs
        val now = System.currentTimeMillis()
        var elapsedMinutes = ((now - trackedStart) / (1000 * 60)).coerceAtLeast(0)

        // Try querying system UsageStats if available
        try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            if (usageStatsManager != null) {
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val startTime = cal.timeInMillis
                val endTime = System.currentTimeMillis()

                val queryStats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    startTime,
                    endTime
                )
                if (!queryStats.isNullOrEmpty()) {
                    var totalTimeMs = 0L
                    for (stat in queryStats) {
                        totalTimeMs += stat.totalTimeInForeground
                    }
                    if (totalTimeMs > 0L) {
                        elapsedMinutes = (totalTimeMs / (1000 * 60)).coerceAtLeast(elapsedMinutes)
                    }
                }
            }
        } catch (e: Exception) {
            // Permission not granted or query unavailable; fallback to tracking session uptime
        }

        val hours = elapsedMinutes / 60
        val mins = elapsedMinutes % 60
        val formattedTime = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

        val focusStatus = when {
            elapsedMinutes < 90 -> "Mindful Focus • Light Screen Use"
            elapsedMinutes < 240 -> "Balanced Activity • Moderate Use"
            else -> "High Engagement • Take a Break"
        }

        // Fluid water level scales with usage (minimum 25%, up to 95%)
        val fluidLevel = (0.25f + (elapsedMinutes / 360f) * 0.70f).coerceIn(0.25f, 0.95f)

        return WellbeingStats(
            screenOnTimeFormatted = formattedTime,
            screenOnMinutes = elapsedMinutes,
            totalUnlocks = unlocks,
            focusStatus = focusStatus,
            fluidContainerPercent = fluidLevel
        )
    }
}
