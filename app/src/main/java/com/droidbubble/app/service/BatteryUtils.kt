package com.droidbubble.app.service

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import kotlin.math.abs
import kotlin.math.roundToInt

object BatteryUtils {

    private const val TAG = "BatteryUtils"
    private const val EXTRA_MAX_CHARGING_CURRENT = "max_charging_current"
    private const val EXTRA_MAX_CHARGING_VOLTAGE = "max_charging_voltage"

    /**
     * Attempts to query charging wattage from system BatteryManager or battery intent.
     * Returns the wattage in Watts (e.g. 4.5, 18.0, 33.0, 65.0) if detected, or null if unavailable.
     */
    fun getChargingWattage(context: Context, batteryIntent: Intent? = null): Double? {
        return try {
            val intent = batteryIntent ?: getBatteryIntent(context)

            // 1. Try MAX charging current and voltage from broadcast extras (Android 9+ / API 28+)
            if (intent != null) {
                val rawCurrent = intent.getIntExtra(EXTRA_MAX_CHARGING_CURRENT, -1)
                val rawVoltage = intent.getIntExtra(EXTRA_MAX_CHARGING_VOLTAGE, -1)

                val amps = normalizeCurrentAmps(rawCurrent)
                val volts = normalizeVoltageVolts(rawVoltage)

                if (amps != null && volts != null) {
                    val watts = amps * volts
                    if (watts in 0.5..250.0) {
                        return (watts * 10.0).roundToInt() / 10.0
                    }
                }

                // If max current is provided but max voltage is missing or 0
                if (amps != null) {
                    val rawCellVoltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
                    val cellVolts = normalizeVoltageVolts(rawCellVoltage) ?: 5.0
                    val effectiveVolts = if (cellVolts in 3.0..4.6) {
                        if (amps <= 1.0) 5.0 else cellVolts
                    } else {
                        cellVolts
                    }
                    val watts = amps * effectiveVolts
                    if (watts in 0.5..250.0) {
                        return (watts * 10.0).roundToInt() / 10.0
                    }
                }
            }

            // 2. Try real-time instantaneous or average current and battery voltage
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            if (bm != null) {
                var rawCurrent = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                if (rawCurrent == 0 || rawCurrent == Int.MIN_VALUE) {
                    rawCurrent = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
                }

                val rawVoltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
                val volts = normalizeVoltageVolts(rawVoltage)
                val amps = normalizeCurrentAmps(abs(rawCurrent))

                if (amps != null && volts != null) {
                    val watts = amps * volts
                    if (watts in 0.5..250.0) {
                        return (watts * 10.0).roundToInt() / 10.0
                    }
                }
            }

            null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get charging wattage", e)
            null
        }
    }

    private fun getBatteryIntent(context: Context): Intent? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    null,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                context.registerReceiver(
                    null,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                )
            }
        } catch (_: Exception) {
            try {
                context.registerReceiver(
                    null,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                )
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun normalizeCurrentAmps(rawCurrent: Int): Double? {
        if (rawCurrent <= 0 || rawCurrent == Int.MIN_VALUE) return null
        return when {
            rawCurrent > 10_000 -> rawCurrent / 1_000_000.0 // Microamperes (uA) -> Amps
            rawCurrent in 50..10_000 -> rawCurrent / 1_000.0 // Milliamperes (mA) -> Amps
            else -> null
        }
    }

    private fun normalizeVoltageVolts(rawVoltage: Int): Double? {
        if (rawVoltage <= 0 || rawVoltage == Int.MIN_VALUE) return null
        return when {
            rawVoltage > 100_000 -> rawVoltage / 1_000_000.0 // Microvolts (uV) -> Volts
            rawVoltage in 1_000..100_000 -> rawVoltage / 1_000.0 // Millivolts (mV) -> Volts
            rawVoltage in 3..25 -> rawVoltage.toDouble() // Direct Volts
            else -> null
        }
    }
}
