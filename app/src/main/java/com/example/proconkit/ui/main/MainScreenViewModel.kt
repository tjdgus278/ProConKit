package com.example.proconkit.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proconkit.sdk.ControllerMonitor
import com.proconkit.sdk.ControllerPowerState
import com.proconkit.sdk.ProControllerState
import com.proconkit.sdk.WirelessHidMonitor
import com.proconkit.sdk.WirelessHidState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

class MainScreenViewModel(
  private val controllerMonitor: ControllerMonitor,
  private val wirelessHidMonitor: WirelessHidMonitor? = null,
  onStateChanged: (ProControllerState) -> Unit = {},
) : ViewModel() {
  private val wirelessStateSource = wirelessHidMonitor?.state ?: MutableStateFlow(WirelessHidState())

  val wirelessState: StateFlow<WirelessHidState> = wirelessStateSource

  val uiState: StateFlow<ProControllerState> =
    combine(controllerMonitor.state, wirelessStateSource, ::mergeWirelessBattery)
      .onEach(onStateChanged)
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = mergeWirelessBattery(controllerMonitor.state.value, wirelessStateSource.value),
      )

  init {
    onStateChanged(uiState.value)
  }

  fun refresh() = controllerMonitor.refresh()

  fun startWirelessDiagnostic() {
    wirelessHidMonitor?.start()
  }

  override fun onCleared() {
    controllerMonitor.close()
    wirelessHidMonitor?.close()
  }

  private fun mergeWirelessBattery(
    controllerState: ProControllerState,
    wirelessState: WirelessHidState,
  ): ProControllerState {
    val batteryPercent = wirelessState.batteryPercent ?: return controllerState
    return controllerState.copy(
      isConnected = true,
      deviceName = wirelessState.deviceName ?: controllerState.deviceName,
      batteryPercent = batteryPercent,
      powerState =
        if (wirelessState.isCharging == true) {
          ControllerPowerState.CHARGING
        } else {
          ControllerPowerState.DISCHARGING
        },
      lastUpdatedAtMillis = System.currentTimeMillis(),
    )
  }
}
