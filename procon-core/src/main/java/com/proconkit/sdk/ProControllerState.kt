package com.proconkit.sdk

/** A point-in-time view of the Nintendo Switch Pro Controller exposed by Android. */
data class ProControllerState(
  val isConnected: Boolean,
  val deviceName: String? = null,
  val batteryPercent: Int? = null,
  val powerState: ControllerPowerState = ControllerPowerState.UNAVAILABLE,
  val lastUpdatedAtMillis: Long = System.currentTimeMillis(),
) {
  companion object {
    fun disconnected(nowMillis: Long = System.currentTimeMillis()) =
      ProControllerState(isConnected = false, lastUpdatedAtMillis = nowMillis)
  }
}

enum class ControllerPowerState {
  CHARGING,
  DISCHARGING,
  FULL,
  NOT_CHARGING,
  UNKNOWN,
  UNAVAILABLE,
}
