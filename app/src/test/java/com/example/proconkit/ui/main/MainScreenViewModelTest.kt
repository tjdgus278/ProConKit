package com.example.proconkit.ui.main

import com.proconkit.sdk.ControllerMonitor
import com.proconkit.sdk.ControllerPowerState
import com.proconkit.sdk.ProControllerState
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Test

class MainScreenViewModelTest {
  @Test
  fun exposesCurrentControllerState() {
    val connected =
      ProControllerState(
        isConnected = true,
        deviceName = "Pro Controller",
        batteryPercent = 75,
        powerState = ControllerPowerState.CHARGING,
      )
    val viewModel = MainScreenViewModel(FakeControllerMonitor(connected))

    assertEquals(connected, viewModel.uiState.value)
  }

  @Test
  fun refreshDelegatesToMonitor() {
    val monitor = FakeControllerMonitor(ProControllerState.disconnected())
    val viewModel = MainScreenViewModel(monitor)

    viewModel.refresh()

    assertTrue(monitor.refreshed)
  }
}

private class FakeControllerMonitor(initialState: ProControllerState) : ControllerMonitor {
  override val state: StateFlow<ProControllerState> = MutableStateFlow(initialState)
  var refreshed = false

  override fun refresh() {
    refreshed = true
  }

  override fun close() = Unit
}
