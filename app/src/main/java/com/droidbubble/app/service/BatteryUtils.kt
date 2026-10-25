package com.droidbubble.app.service

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import kotlin.math.abs

object BatteryUtils {

    private const val EXTRA_MAX_CHARGING_CURRENT = "max_charging_current"
    private const val EXTRA_MAX_CHARGING_VOLTAGE = "max_charging_voltage"

    /**
     * Attempts to query charging wattage from system BatteryManager or battery intent.
     * Returns the wattage in Watts (e.g. 18.0, 33.0, 65.0) if detected, or null if unavailable.
     */
    fun getChargingWattage(context: Context, batteryIntent: Intent? = null): Double? {
        return try {
            val intent = batteryIntent ?: context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )

            // 1. Try MAX charging current and voltage (Android 9+ / API 28+)
            if (intent != null) {
                val maxCurrentUa = intent.getIntExtra(EXTRA_MAX_CHARGING_CURRENT, -1)
                val maxVoltageUv = intent.getIntExtra(EXTRA_MAX_CHARGING_VOLTAGE, -1)

                if (maxCurrentUa > 0 && maxVoltageUv > 0) {
                    val amps = maxCurrentUa / 1_000_000.0
                    val volts = maxVoltageUv / 1_000_000.0
                    val watts = amps * volts
                    if (watts in 1.0..250.0) {
                        return watts
                    }
                }
            }

            // 2. Try real-time instantaneous current and voltage
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            if (bm != null && intent != null) {
                val currentNowUa = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)

                if (currentNowUa != 0 && currentNowUa != Int.MIN_VALUE && voltageMv > 0) {
                    val amps = abs(currentNowUa) / 1_000_000.0
                    val volts = voltageMv / 1_000.0
                    val watts = amps * volts
                    if (watts in 1.0..250.0) {
                        return watts
                    }
                }
            }

            // 3. Fallback: if max current is available with nominal voltage
            if (intent != null) {
                val maxCurrentUa = intent.getIntExtra(EXTRA_MAX_CHARGING_CURRENT, -1)
                val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 5000)
                if (maxCurrentUa > 0) {
                    val amps = maxCurrentUa / 1_000_000.0
                    val volts = (if (voltageMv > 0) voltageMv else 5000) / 1_000.0
                    val watts = amps * volts
                    if (watts in 1.0..250.0) {
                        return watts
                    }
                }
            }

            null
        } catch (_: Exception) {
            null
        }
    }
}
