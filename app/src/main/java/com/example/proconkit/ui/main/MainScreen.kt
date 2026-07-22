package com.example.proconkit.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proconkit.R
import com.example.proconkit.data.ControllerStateStore
import com.example.proconkit.theme.ProConKitTheme
import com.example.proconkit.widget.ControllerWidgetUpdater
import com.proconkit.sdk.ControllerPowerState
import com.proconkit.sdk.ProControllerMonitor
import com.proconkit.sdk.ProControllerState

@Composable
fun MainScreen(
  modifier: Modifier = Modifier,
) {
  val appContext = LocalContext.current.applicationContext
  val viewModel: MainScreenViewModel =
    viewModel {
      val stateStore = ControllerStateStore(appContext)
      MainScreenViewModel(
        controllerMonitor = ProControllerMonitor(appContext),
        onStateChanged = { state ->
          stateStore.save(state)
          ControllerWidgetUpdater.updateAll(appContext, state)
        },
      )
    }
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  MainScreen(state = state, onRefresh = viewModel::refresh, modifier = modifier)
}

@Composable
internal fun MainScreen(
  state: ProControllerState,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(20.dp),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = stringResource(R.string.app_subtitle),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyLarge,
      )
    }

    ControllerStatusCard(state = state)

    Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
      Text(stringResource(R.string.refresh_status))
    }

    Card(
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
      Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = stringResource(R.string.widget_title),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = stringResource(R.string.widget_hint),
          color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
      }
    }
  }
}

@Composable
private fun ControllerStatusCard(state: ProControllerState) {
  val statusColor =
    if (state.isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
  ) {
    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = state.deviceName ?: stringResource(R.string.pro_controller),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            text =
              if (state.isConnected) {
                stringResource(R.string.connected)
              } else {
                stringResource(R.string.disconnected)
              },
            color = statusColor,
            fontWeight = FontWeight.Bold,
          )
        }
      }

      if (state.isConnected) {
        val batteryPercent = state.batteryPercent
        val batteryText =
          batteryPercent?.let { stringResource(R.string.battery_percent, it) }
            ?: stringResource(R.string.battery_unavailable)
        Text(
          text = batteryText,
          style = MaterialTheme.typography.displaySmall,
          fontWeight = FontWeight.Bold,
        )
        if (batteryPercent != null) {
          LinearProgressIndicator(
            progress = { batteryPercent / 100f },
            modifier = Modifier.fillMaxWidth(),
          )
        }
        Text(
          text = powerStateLabel(state.powerState),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        Text(
          text = stringResource(R.string.connect_instruction),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun powerStateLabel(powerState: ControllerPowerState): String =
  when (powerState) {
    ControllerPowerState.CHARGING -> stringResource(R.string.charging)
    ControllerPowerState.DISCHARGING -> stringResource(R.string.on_battery)
    ControllerPowerState.FULL -> stringResource(R.string.fully_charged)
    ControllerPowerState.NOT_CHARGING -> stringResource(R.string.not_charging)
    ControllerPowerState.UNKNOWN -> stringResource(R.string.charging_unknown)
    ControllerPowerState.UNAVAILABLE -> stringResource(R.string.battery_api_unavailable)
  }

@Preview(showBackground = true)
@Composable
private fun ConnectedPreview() {
  ProConKitTheme {
    Surface {
      MainScreen(
        state =
          ProControllerState(
            isConnected = true,
            deviceName = "Pro Controller",
            batteryPercent = 75,
            powerState = ControllerPowerState.CHARGING,
          ),
        onRefresh = {},
        modifier = Modifier.padding(20.dp),
      )
    }
  }
}
