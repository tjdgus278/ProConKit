package com.proconkit.sdk

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.lsposed.hiddenapibypass.HiddenApiBypass

interface WirelessHidMonitor : AutoCloseable {
  val state: StateFlow<WirelessHidState>

  fun start()
}

/**
 * Experimental Android 12-16 transport using Android's hidden Bluetooth HID Host profile.
 * Every vendor-dependent failure is surfaced through [state] for on-device diagnosis.
 */
@SuppressLint("NewApi", "PrivateApi")
class ExperimentalBluetoothHidMonitor(context: Context) :
  WirelessHidMonitor,
  BluetoothProfile.ServiceListener {
  private val appContext = context.applicationContext
  private val bluetoothAdapter = appContext.getSystemService(BluetoothManager::class.java)?.adapter
  private val handler = Handler(Looper.getMainLooper())
  private val mutableState = MutableStateFlow(WirelessHidState())
  private var profileProxy: BluetoothProfile? = null
  private var receiverRegistered = false
  private var packetNumber = 0

  override val state: StateFlow<WirelessHidState> = mutableState.asStateFlow()

  private val reportReceiver =
    object : BroadcastReceiver() {
      override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
          ACTION_REPORT -> handleReport(intent)
          ACTION_HANDSHAKE -> handleHandshake(intent)
        }
      }
    }

  override fun start() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
      fail("The experimental wireless HID path requires Android 9 or newer")
      return
    }
    if (!hasBluetoothPermission()) {
      mutableState.value = WirelessHidState(status = WirelessHidStatus.PERMISSION_REQUIRED)
      return
    }

    val adapter = bluetoothAdapter
    if (adapter == null) {
      mutableState.value = WirelessHidState(status = WirelessHidStatus.BLUETOOTH_UNAVAILABLE)
      return
    }
    if (!adapter.isEnabled) {
      mutableState.value = WirelessHidState(status = WirelessHidStatus.BLUETOOTH_OFF)
      return
    }

    registerReceiverIfNeeded()
    closeProfileProxy()
    mutableState.value =
      WirelessHidState(
        status = WirelessHidStatus.OPENING_PROFILE,
        detail = "Requesting Android's hidden HID_HOST profile",
      )

    try {
      HiddenApiBypass.addHiddenApiExemptions("Landroid/bluetooth/BluetoothHidHost;")
      val accepted = adapter.getProfileProxy(appContext, this, HID_HOST_PROFILE)
      if (!accepted) fail("BluetoothAdapter rejected the HID_HOST profile request")
    } catch (error: Throwable) {
      fail("Hidden HID profile access failed", error)
    }
  }

  @SuppressLint("MissingPermission")
  override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
    if (profile != HID_HOST_PROFILE) return
    profileProxy = proxy

    try {
      val matchingController =
        proxy.connectedDevices.firstOrNull { device ->
          val name = device.name.orEmpty()
          name.contains("Pro Controller", ignoreCase = true) ||
            (name.contains("Nintendo", ignoreCase = true) && name.contains("Switch", ignoreCase = true))
        }
      if (matchingController == null) {
        mutableState.value =
          WirelessHidState(
            status = WirelessHidStatus.CONTROLLER_NOT_FOUND,
            detail = "HID_HOST opened, but no connected Pro Controller was returned",
          )
        return
      }

      requestBattery(matchingController)
    } catch (error: Throwable) {
      fail("Could not enumerate connected HID devices", error)
    }
  }

  override fun onServiceDisconnected(profile: Int) {
    if (profile != HID_HOST_PROFILE) return
    profileProxy = null
    if (mutableState.value.status != WirelessHidStatus.BATTERY_RECEIVED) {
      fail("Android disconnected the HID_HOST profile")
    }
  }

  @SuppressLint("MissingPermission")
  private fun requestBattery(device: BluetoothDevice) {
    val proxy = profileProxy ?: return fail("HID_HOST profile proxy is missing")
    val deviceName = runCatching { device.name }.getOrNull() ?: "Pro Controller"
    val command =
      NintendoHidProtocol.subcommandHex(
        packetNumber = packetNumber++ and 0x0F,
        subcommand = NintendoHidProtocol.SUBCOMMAND_REQUEST_DEVICE_INFO,
      )

    try {
      val hidHostClass = Class.forName(HID_HOST_CLASS)
      val commandAccepted =
        HiddenApiBypass.invoke(hidHostClass, proxy, "sendData", device, command) as? Boolean ?: false
      mutableState.value =
        WirelessHidState(
          status = WirelessHidStatus.REQUESTING_REPORT,
          deviceName = deviceName,
          detail =
            if (commandAccepted) {
              "Device-info subcommand accepted; requesting feature report 0x02"
            } else {
              "sendData was rejected; trying cached feature/input reports"
            },
        )

      handler.postDelayed({ requestReport(proxy, device, REPORT_TYPE_FEATURE, 0x02) }, 180L)
      handler.postDelayed({ requestReport(proxy, device, REPORT_TYPE_INPUT, 0x21) }, 360L)
      handler.postDelayed({ requestReport(proxy, device, REPORT_TYPE_INPUT, 0x30) }, 540L)
      handler.postDelayed(
        {
          if (mutableState.value.status == WirelessHidStatus.REQUESTING_REPORT) {
            mutableState.value =
              mutableState.value.copy(
                status = WirelessHidStatus.NO_REPORT,
                detail = "Commands were issued, but Samsung did not deliver a HID report within 5 seconds",
              )
          }
        },
        REPORT_TIMEOUT_MILLIS,
      )
    } catch (error: Throwable) {
      fail("Hidden BluetoothHidHost method invocation failed", error, deviceName)
    }
  }

  private fun requestReport(proxy: BluetoothProfile, device: BluetoothDevice, type: Byte, id: Int) {
    if (mutableState.value.status == WirelessHidStatus.BATTERY_RECEIVED) return
    try {
      val accepted =
        HiddenApiBypass.invoke(
          Class.forName(HID_HOST_CLASS),
          proxy,
          "getReport",
          device,
          type,
          id.toByte(),
          REPORT_BUFFER_SIZE,
        ) as? Boolean ?: false
      if (!accepted) {
        mutableState.value =
          mutableState.value.copy(detail = "Android rejected report 0x${id.toString(16).uppercase()}")
      }
    } catch (error: Throwable) {
      fail("getReport(0x${id.toString(16).uppercase()}) failed", error, mutableState.value.deviceName)
    }
  }

  private fun handleReport(intent: Intent) {
    val report =
      intent.getByteArrayExtra(EXTRA_REPORT)
        ?: intent.getByteArrayExtra(LEGACY_EXTRA_REPORT)
        ?: return
    val reportHex = NintendoHidProtocol.run { report.toHexString() }
    val reading = NintendoHidProtocol.parseBattery(report)
    if (reading == null) {
      mutableState.value =
        mutableState.value.copy(
          rawReportHex = reportHex,
          detail = "A HID report arrived, but it did not contain a Nintendo battery header",
        )
      return
    }

    handler.removeCallbacksAndMessages(null)
    mutableState.value =
      WirelessHidState(
        status = WirelessHidStatus.BATTERY_RECEIVED,
        deviceName = mutableState.value.deviceName,
        batteryPercent = reading.percent,
        isCharging = reading.isCharging,
        rawReportHex = reportHex,
        detail =
          "Decoded bat_con: level=${reading.rawLevel}, connection=0x${reading.connectionInfo.toString(16).uppercase()}",
      )
  }

  private fun handleHandshake(intent: Intent) {
    val status = intent.getIntExtra(EXTRA_STATUS, -1)
    if (mutableState.value.status != WirelessHidStatus.BATTERY_RECEIVED) {
      mutableState.value = mutableState.value.copy(detail = "HID handshake status: $status")
    }
  }

  private fun registerReceiverIfNeeded() {
    if (receiverRegistered) return
    val filter =
      IntentFilter().apply {
        addAction(ACTION_REPORT)
        addAction(ACTION_HANDSHAKE)
      }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      appContext.registerReceiver(reportReceiver, filter, Context.RECEIVER_EXPORTED)
    } else {
      @Suppress("DEPRECATION") appContext.registerReceiver(reportReceiver, filter)
    }
    receiverRegistered = true
  }

  private fun hasBluetoothPermission(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
      appContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

  private fun fail(message: String, error: Throwable? = null, deviceName: String? = null) {
    mutableState.value =
      WirelessHidState(
        status = WirelessHidStatus.ERROR,
        deviceName = deviceName,
        detail = if (error == null) message else "$message: ${error.javaClass.simpleName}: ${error.message}",
      )
  }

  private fun closeProfileProxy() {
    val proxy = profileProxy ?: return
    runCatching { bluetoothAdapter?.closeProfileProxy(HID_HOST_PROFILE, proxy) }
    profileProxy = null
  }

  override fun close() {
    handler.removeCallbacksAndMessages(null)
    closeProfileProxy()
    if (receiverRegistered) {
      runCatching { appContext.unregisterReceiver(reportReceiver) }
      receiverRegistered = false
    }
  }

  private companion object {
    const val HID_HOST_PROFILE = 4
    const val HID_HOST_CLASS = "android.bluetooth.BluetoothHidHost"
    const val ACTION_REPORT = "android.bluetooth.input.profile.action.REPORT"
    const val ACTION_HANDSHAKE = "android.bluetooth.input.profile.action.HANDSHAKE"
    const val EXTRA_REPORT = "android.bluetooth.BluetoothHidHost.extra.REPORT"
    const val LEGACY_EXTRA_REPORT = "android.bluetooth.BluetoothInputDevice.extra.REPORT"
    const val EXTRA_STATUS = "android.bluetooth.BluetoothHidHost.extra.STATUS"
    const val REPORT_TYPE_INPUT: Byte = 1
    const val REPORT_TYPE_FEATURE: Byte = 3
    const val REPORT_BUFFER_SIZE = 64
    const val REPORT_TIMEOUT_MILLIS = 5_000L
  }
}
