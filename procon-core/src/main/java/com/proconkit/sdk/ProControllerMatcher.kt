package com.proconkit.sdk

import android.view.InputDevice

/** Identifies an official Nintendo Switch Pro Controller from Android input metadata. */
object ProControllerMatcher {
  const val NINTENDO_VENDOR_ID = 0x057E
  const val PRO_CONTROLLER_PRODUCT_ID = 0x2009

  fun matches(vendorId: Int, productId: Int, name: String, sources: Int): Boolean {
    val knownUsbIdentity = vendorId == NINTENDO_VENDOR_ID && productId == PRO_CONTROLLER_PRODUCT_ID
    val isGameController =
      sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
        sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
    val normalizedName = name.lowercase()
    val recognizableName =
      normalizedName.contains("pro controller") ||
        (normalizedName.contains("nintendo") && normalizedName.contains("switch"))

    return knownUsbIdentity || (isGameController && recognizableName)
  }
}
