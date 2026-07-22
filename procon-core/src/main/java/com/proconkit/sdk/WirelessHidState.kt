package com.proconkit.sdk

data class WirelessHidState(
  val status: WirelessHidStatus = WirelessHidStatus.IDLE,
  val deviceName: String? = null,
  val batteryPercent: Int? = null,
  val isCharging: Boolean? = null,
  val rawReportHex: String? = null,
  val detail: String? = null,
)

enum class WirelessHidStatus {
  IDLE,
  PERMISSION_REQUIRED,
  BLUETOOTH_UNAVAILABLE,
  BLUETOOTH_OFF,
  OPENING_PROFILE,
  CONTROLLER_NOT_FOUND,
  REQUESTING_REPORT,
  BATTERY_RECEIVED,
  NO_REPORT,
  ERROR,
}
