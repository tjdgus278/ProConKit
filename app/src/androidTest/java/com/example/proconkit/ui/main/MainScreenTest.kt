package com.example.proconkit.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.proconkit.R
import com.proconkit.sdk.ControllerPowerState
import com.proconkit.sdk.ProControllerState
import org.junit.Rule
import org.junit.Test

class MainScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun connectedControllerStatusIsDisplayed() {
    val state =
      ProControllerState(
        isConnected = true,
        deviceName = "Pro Controller",
        batteryPercent = 75,
        powerState = ControllerPowerState.CHARGING,
      )
    composeTestRule.setContent { MainScreen(state = state, onRefresh = {}) }

    composeTestRule.onNodeWithText("Pro Controller").assertExists()
    composeTestRule.onNodeWithText("75%").assertExists()
    composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.connected)).assertExists()
  }
}
