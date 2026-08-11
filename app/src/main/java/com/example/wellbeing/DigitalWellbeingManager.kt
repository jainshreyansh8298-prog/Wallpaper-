package com.example.wellbeing

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import com.example.wallpaper.WallpaperSettingsStore
import java.util.Calendar

data class WellbeingStats(
    val isPermissionGranted: Boolean,
    val screenOnTimeFormatted: String,
    val screenOnMinutes: Long,
    val totalUnlocks: Int,
    val focusStatus: String,
    val fluidContainerPercent: Float
)

object DigitalWellbeingManager {

    fun hasUsageStatsPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            val mode = appOps?.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    fun getUsageAccessSettingsIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    }

    fun getStats(context: Context): WellbeingStats {
        val isGranted = hasUsageStatsPermission(context)
        val config = WallpaperSettingsStore.loadConfig(context)

        var totalMinutes = 0L
        var unlockEvents = config.unlockCount.coerceAtLeast(1)

        if (isGranted) {
            try {
                val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                if (usageStatsManager != null) {
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)

                    val startTime = cal.timeInMillis
                    val endTime = System.currentTimeMillis()

                    // Query real daily total time in foreground across all apps
                    val stats = usageStatsManager.queryUsageStats(
                        UsageStatsManager.INTERVAL_DAILY,
                        startTime,
                        endTime
                    )

                    if (!stats.isNullOrEmpty()) {
                        var totalTimeMs = 0L
                        for (s in stats) {
                            if (s.totalTimeInForeground > 0) {
                                totalTimeMs += s.totalTimeInForeground
                            }
                        }
                        if (totalTimeMs > 0) {
                            totalMinutes = totalTimeMs / (1000 * 60)
                        }
                    }

                    // Query real keyguard / app foreground launch events from system usage events
                    val events = usageStatsManager.queryEvents(startTime, endTime)
                    var countEvents = 0
                    if (events != null) {
                        val event = UsageEvents.Event()
                        while (events.hasNextEvent()) {
                            events.getNextEvent(event)
                            // EventType 1 = MOVE_TO_FOREGROUND, 15 = SCREEN_INTERACTED, 26 = KEYGUARD_DISMISSED
                            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                                event.eventType == 15 ||
                                event.eventType == 26) {
                                countEvents++
                            }
                        }
                    }
                    if (countEvents > 0) {
                        unlockEvents = countEvents
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // Fallback session uptime tracker when system permission is pending
            val elapsedSec = ((System.currentTimeMillis() - config.trackedTimeStartMs) / 1000L).coerceAtLeast(0L)
            totalMinutes = elapsedSec / 60
        }

        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        val formattedTime = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

        val focusStatus = when {
            !isGranted -> "Grant Permission to Sync System Wellbeing"
            totalMinutes < 90 -> "Mindful Focus • Light Usage"
            totalMinutes < 240 -> "Balanced Daily Screen Time"
            else -> "High Device Engagement • Take a Break"
        }

        val fluidLevel = (0.25f + (totalMinutes / 360f) * 0.70f).coerceIn(0.25f, 0.95f)

        return WellbeingStats(
            isPermissionGranted = isGranted,
            screenOnTimeFormatted = formattedTime,
            screenOnMinutes = totalMinutes,
            totalUnlocks = unlockEvents,
            focusStatus = focusStatus,
            fluidContainerPercent = fluidLevel
        )
    }
}
