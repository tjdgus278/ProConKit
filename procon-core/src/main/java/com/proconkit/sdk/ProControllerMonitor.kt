package com.proconkit.sdk

import android.content.Context
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ControllerMonitor : AutoCloseable {
  val state: StateFlow<ProControllerState>

  fun refresh()
}

/** Observes Android input-device events and publishes the latest Pro Controller state. */
class ProControllerMonitor(context: Context) : ControllerMonitor, InputManager.InputDeviceListener {
  private val appContext = context.applicationContext
  private val inputManager = appContext.getSystemService(InputManager::class.java)
  private val mutableState = MutableStateFlow(ProControllerDiscovery.snapshot(appContext))

  override val state: StateFlow<ProControllerState> = mutableState.asStateFlow()

  init {
    inputManager.registerInputDeviceListener(this, Handler(Looper.getMainLooper()))
  }

  override fun refresh() {
    mutableState.value = ProControllerDiscovery.snapshot(appContext)
  }

  override fun onInputDeviceAdded(deviceId: Int) = refresh()

  override fun onInputDeviceRemoved(deviceId: Int) = refresh()

  override fun onInputDeviceChanged(deviceId: Int) = refresh()

  override fun close() {
    inputManager.unregisterInputDeviceListener(this)
  }
}
