package com.proconkit.sdk

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Test

class NintendoHidProtocolTest {
  @Test
  fun buildsDeviceInfoSubcommand() {
    assertEquals(
      "0103000140400001404002",
      NintendoHidProtocol.subcommandHex(3, NintendoHidProtocol.SUBCOMMAND_REQUEST_DEVICE_INFO),
    )
  }

  @Test
  fun parsesFullChargingBatteryFromInputReport() {
    val reading = NintendoHidProtocol.parseBattery(byteArrayOf(0x21, 0x10, 0x90.toByte()))

    assertEquals(100, reading?.percent)
    assertTrue(reading?.isCharging == true)
  }

  @Test
  fun parsesWrappedFeatureReport() {
    val reading = NintendoHidProtocol.parseBattery(byteArrayOf(0x02, 0x21, 0x10, 0x60))

    assertEquals(75, reading?.percent)
    assertFalse(reading?.isCharging ?: true)
  }

  @Test
  fun rejectsBasicButtonReportWithoutBatteryHeader() {
    assertNull(NintendoHidProtocol.parseBattery(byteArrayOf(0x3F, 0x00, 0x80.toByte())))
  }
}
