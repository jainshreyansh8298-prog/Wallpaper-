package com.example.celestial

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object DynamicIconManager {

    private const val ALIAS_SUNRISE = "com.example.MainActivitySunrise"
    private const val ALIAS_NOON = "com.example.MainActivityNoon"
    private const val ALIAS_AFTERNOON = "com.example.MainActivityAfternoon"
    private const val ALIAS_NIGHT = "com.example.MainActivityNight"

    fun getCurrentActiveIconType(context: Context): TimeOfDayType {
        val pm = context.packageManager
        val timeOfDay = CelestialManager.getCelestialInfo().timeOfDay

        val sunriseEnabled = pm.getComponentEnabledSetting(ComponentName(context, ALIAS_SUNRISE)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        val noonEnabled = pm.getComponentEnabledSetting(ComponentName(context, ALIAS_NOON)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        val afternoonEnabled = pm.getComponentEnabledSetting(ComponentName(context, ALIAS_AFTERNOON)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        val nightEnabled = pm.getComponentEnabledSetting(ComponentName(context, ALIAS_NIGHT)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED

        return when {
            sunriseEnabled -> TimeOfDayType.SUNRISE
            noonEnabled -> TimeOfDayType.NOON
            afternoonEnabled -> TimeOfDayType.AFTERNOON
            nightEnabled -> TimeOfDayType.NIGHT
            else -> timeOfDay
        }
    }

    fun applyDynamicIcon(context: Context, timeOfDay: TimeOfDayType) {
        val pm = context.packageManager

        val sunriseComponent = ComponentName(context, ALIAS_SUNRISE)
        val noonComponent = ComponentName(context, ALIAS_NOON)
        val afternoonComponent = ComponentName(context, ALIAS_AFTERNOON)
        val nightComponent = ComponentName(context, ALIAS_NIGHT)

        try {
            // Enable target component and disable others
            val setEnabled = { comp: ComponentName, enable: Boolean ->
                val state = if (enable) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                pm.setComponentEnabledSetting(comp, state, PackageManager.DONT_KILL_APP)
            }

            setEnabled(sunriseComponent, timeOfDay == TimeOfDayType.SUNRISE)
            setEnabled(noonComponent, timeOfDay == TimeOfDayType.NOON)
            setEnabled(afternoonComponent, timeOfDay == TimeOfDayType.AFTERNOON)
            setEnabled(nightComponent, timeOfDay == TimeOfDayType.NIGHT)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
