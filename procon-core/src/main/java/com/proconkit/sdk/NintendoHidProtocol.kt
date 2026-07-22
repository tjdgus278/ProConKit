package com.proconkit.sdk

/** Minimal Nintendo HID protocol helpers used by the experimental wireless transport. */
object NintendoHidProtocol {
  private const val OUTPUT_RUMBLE_AND_SUBCOMMAND = 0x01
  private val neutralRumble = byteArrayOf(0x00, 0x01, 0x40, 0x40, 0x00, 0x01, 0x40, 0x40)

  const val SUBCOMMAND_REQUEST_DEVICE_INFO = 0x02
  const val FEATURE_LAST_SUBCOMMAND_REPLY = 0x02

  fun subcommandHex(packetNumber: Int, subcommand: Int, data: ByteArray = byteArrayOf()): String {
    require(packetNumber in 0..15) { "Packet number must fit in four bits" }
    require(subcommand in 0..255) { "Subcommand must fit in one byte" }

    val report =
      byteArrayOf(OUTPUT_RUMBLE_AND_SUBCOMMAND.toByte(), packetNumber.toByte()) +
        neutralRumble +
        byteArrayOf(subcommand.toByte()) +
        data
    // Android's Bluetooth stack parses this String as contiguous ASCII hex.
    return report.joinToString(separator = "") { byte -> "%02X".format(byte.toInt() and 0xFF) }
  }

  fun parseBattery(report: ByteArray): NintendoBatteryReading? {
    // Feature report 0x02 wraps the most recent 0x21 input report. Some stacks retain
    // the feature ID as the first byte, so locate the embedded Nintendo input report.
    val reportStart =
      listOf(0, 1).filter { it <= report.lastIndex }.firstOrNull { index ->
        report[index].toInt() and 0xFF in BATTERY_REPORT_IDS
      } ?: return null
    if (report.size <= reportStart + BATTERY_BYTE_OFFSET) return null

    val batteryAndConnection = report[reportStart + BATTERY_BYTE_OFFSET].toInt() and 0xFF
    val level = (batteryAndConnection ushr 5) and 0x07
    val percent = level.coerceAtMost(MAX_BATTERY_LEVEL) * 100 / MAX_BATTERY_LEVEL
    return NintendoBatteryReading(
      percent = percent,
      isCharging = batteryAndConnection and CHARGING_MASK != 0,
      rawLevel = level,
      connectionInfo = batteryAndConnection and CONNECTION_MASK,
    )
  }

  fun ByteArray.toHexString(): String =
    joinToString(separator = " ") { byte -> "%02X".format(byte.toInt() and 0xFF) }

  private const val BATTERY_BYTE_OFFSET = 2
  private const val MAX_BATTERY_LEVEL = 4
  private const val CHARGING_MASK = 0x10
  private const val CONNECTION_MASK = 0x0F
  private val BATTERY_REPORT_IDS = setOf(0x21, 0x30, 0x31)
}

data class NintendoBatteryReading(
  val percent: Int,
  val isCharging: Boolean,
  val rawLevel: Int,
  val connectionInfo: Int,
)
