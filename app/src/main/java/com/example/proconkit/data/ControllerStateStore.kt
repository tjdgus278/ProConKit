package com.example.proconkit.data

import android.content.Context
import androidx.core.content.edit
import com.proconkit.sdk.ControllerPowerState
import com.proconkit.sdk.ProControllerState

class ControllerStateStore(context: Context) {
  private val preferences =
    context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

  fun save(state: ProControllerState) {
    preferences.edit {
      putBoolean(KEY_CONNECTED, state.isConnected)
      putString(KEY_DEVICE_NAME, state.deviceName)
      putInt(KEY_BATTERY_PERCENT, state.batteryPercent ?: BATTERY_UNAVAILABLE)
      putString(KEY_POWER_STATE, state.powerState.name)
      putLong(KEY_UPDATED_AT, state.lastUpdatedAtMillis)
    }
  }

  fun load(): ProControllerState {
    if (!preferences.contains(KEY_UPDATED_AT)) return ProControllerState.disconnected()

    val storedBattery = preferences.getInt(KEY_BATTERY_PERCENT, BATTERY_UNAVAILABLE)
    val powerState =
      runCatching {
          ControllerPowerState.valueOf(
            preferences.getString(KEY_POWER_STATE, ControllerPowerState.UNAVAILABLE.name).orEmpty()
          )
        }
        .getOrDefault(ControllerPowerState.UNAVAILABLE)

    return ProControllerState(
      isConnected = preferences.getBoolean(KEY_CONNECTED, false),
      deviceName = preferences.getString(KEY_DEVICE_NAME, null),
      batteryPercent = storedBattery.takeUnless { it == BATTERY_UNAVAILABLE },
      powerState = powerState,
      lastUpdatedAtMillis = preferences.getLong(KEY_UPDATED_AT, System.currentTimeMillis()),
    )
  }

  private companion object {
    const val PREFERENCES_NAME = "controller_state"
    const val KEY_CONNECTED = "connected"
    const val KEY_DEVICE_NAME = "device_name"
    const val KEY_BATTERY_PERCENT = "battery_percent"
    const val KEY_POWER_STATE = "power_state"
    const val KEY_UPDATED_AT = "updated_at"
    const val BATTERY_UNAVAILABLE = -1
  }
}
