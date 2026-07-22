package com.example.proconkit.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proconkit.sdk.ControllerMonitor
import com.proconkit.sdk.ProControllerState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

class MainScreenViewModel(
  private val controllerMonitor: ControllerMonitor,
  onStateChanged: (ProControllerState) -> Unit = {},
) : ViewModel() {
  val uiState: StateFlow<ProControllerState> =
    controllerMonitor.state
      .onEach(onStateChanged)
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = controllerMonitor.state.value,
      )

  init {
    onStateChanged(controllerMonitor.state.value)
  }

  fun refresh() = controllerMonitor.refresh()

  override fun onCleared() {
    controllerMonitor.close()
  }
}
