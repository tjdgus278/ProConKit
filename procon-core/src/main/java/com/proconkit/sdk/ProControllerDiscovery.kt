package com.proconkit.sdk

import android.content.Context
import android.annotation.TargetApi
import android.hardware.BatteryState
import android.hardware.input.InputManager
import android.os.Build
import android.view.InputDevice
import kotlin.math.roundToInt

/** One-shot discovery for app widgets and clients that do not need continuous monitoring. */
object ProControllerDiscovery {
  fun snapshot(context: Context): ProControllerState {
    val inputManager = context.getSystemService(InputManager::class.java)
    val candidates =
      inputManager.inputDeviceIds
        .asSequence()
        .mapNotNull(inputManager::getInputDevice)
        .filter { device ->
          ProControllerMatcher.matches(
            vendorId = device.vendorId,
            productId = device.productId,
            name = device.name,
            sources = device.sources,
          )
        }

    val device =
      candidates.maxByOrNull { candidate ->
        if (
          candidate.vendorId == ProControllerMatcher.NINTENDO_VENDOR_ID &&
            candidate.productId == ProControllerMatcher.PRO_CONTROLLER_PRODUCT_ID
        ) {
          1
        } else {
          0
        }
      } ?: return ProControllerState.disconnected()

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      stateWithBattery(device)
    } else {
      ProControllerState(
        isConnected = true,
        deviceName = device.name,
        powerState = ControllerPowerState.UNAVAILABLE,
      )
    }
  }

  @TargetApi(Build.VERSION_CODES.S)
  private fun stateWithBattery(device: InputDevice): ProControllerState {
    val battery = device.batteryState
    if (!battery.isPresent) {
      return ProControllerState(
        isConnected = true,
        deviceName = device.name,
        powerState = ControllerPowerState.UNAVAILABLE,
      )
    }

    val batteryPercent =
      battery.capacity
        .takeUnless(Float::isNaN)
        ?.coerceIn(0f, 1f)
        ?.times(100)
        ?.roundToInt()

    val powerState =
      when (battery.status) {
        BatteryState.STATUS_CHARGING -> ControllerPowerState.CHARGING
        BatteryState.STATUS_DISCHARGING -> ControllerPowerState.DISCHARGING
        BatteryState.STATUS_FULL -> ControllerPowerState.FULL
        BatteryState.STATUS_NOT_CHARGING -> ControllerPowerState.NOT_CHARGING
        else -> ControllerPowerState.UNKNOWN
      }

    return ProControllerState(
      isConnected = true,
      deviceName = device.name,
      batteryPercent = batteryPercent,
      powerState = powerState,
    )
  }
}
