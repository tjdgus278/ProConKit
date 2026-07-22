package com.proconkit.sdk

import android.view.InputDevice
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

class ProControllerMatcherTest {
  @Test
  fun matchesOfficialUsbIdentity() {
    assertTrue(
      ProControllerMatcher.matches(
        vendorId = ProControllerMatcher.NINTENDO_VENDOR_ID,
        productId = ProControllerMatcher.PRO_CONTROLLER_PRODUCT_ID,
        name = "Unknown device",
        sources = 0,
      )
    )
  }

  @Test
  fun matchesBluetoothNameWhenDeviceIsGamepad() {
    assertTrue(
      ProControllerMatcher.matches(
        vendorId = 0,
        productId = 0,
        name = "Pro Controller",
        sources = InputDevice.SOURCE_GAMEPAD,
      )
    )
  }

  @Test
  fun rejectsUnrelatedGamepad() {
    assertFalse(
      ProControllerMatcher.matches(
        vendorId = 0x045E,
        productId = 0x0B13,
        name = "Wireless Controller",
        sources = InputDevice.SOURCE_GAMEPAD,
      )
    )
  }
}
